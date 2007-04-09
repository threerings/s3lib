/* 
 * DownloadStreamer.java vi:ts=4:sw=4:expandtab:
 *
 * Copyright (c) 2007 Three Rings Design, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright owner nor the names of contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.threerings.s3.pipe;

import com.threerings.s3.client.acl.AccessControlList;
import com.threerings.s3.client.S3ByteArrayObject;
import com.threerings.s3.client.S3Connection;
import com.threerings.s3.client.S3Exception;
import com.threerings.s3.client.S3Object;
import com.threerings.s3.client.S3ServerException;

import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.ByteBuffer;

import java.util.Arrays;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/*
 * Downloads a series of S3Objects and re-assembles them as a stream.
 */
class DownloadStreamer {
    /*
     * Instantiate a new stream downloader.
     * @param connect: S3 Connection.
     * @param bucket: Destination S3 bucket.
     */
    public DownloadStreamer (S3Connection connection, String bucket)
    {
        _connection = connection;
        _bucket = bucket;
    }

    /**
     * Download the given streamName.
     */
    public void download (String streamName, OutputStream output, int maxRetry)
        throws S3Exception, RemoteStreamException
    {
        RemoteStream stream;
        RemoteStreamInfo info;

        stream = new RemoteStream(_connection, _bucket, streamName);

        /* Fetch the stream info record. The caller must handle any RemoteStreamExceptions */
        try {
            S3RetryHandler retry = new S3RetryHandler(maxRetry);
            S3Exception retryError = null;

            do {
                /* Log the last error. */
                if (retryError != null) {
                    System.err.println("S3 error occured creating stream info record, retrying: " + retryError);                    
                }

                try {
                    /* Fetch the stream info */
                    info = stream.getStreamInfo();
                    
                    if (info == null) {
                        throw new RemoteStreamException.NoSuchStreamException("Stream \"" + streamName + "\" does not exist.");
                    }             
                } catch (S3Exception s3e) {
                    /* Let the retry handler check the exception */
                    retryError = s3e;
                    continue;
                }

                break;
            } while (retry.shouldRetry(retryError));
        } catch (S3Exception s3e) {
            throw new RemoteStreamException("S3 failure fetching stream info record for '" +
                streamName + "': " + s3e.getMessage());            
        }

        /*
         * Download the blocks from S3. We use a 32k read buffer.
         */
        byte[] buffer = new byte[BUFFER_SIZE];
        for (long blockId = 0; ; blockId++) {
            /* Fetch the next remote block, write it to the output stream */
            try {
                S3Object block;
                MessageDigest blockDigest;
                InputStream input;
                boolean eof;
                int nread;

                /* Fetch the block from the remote host. */
                block = fetchRemoteBlock(stream, blockId, maxRetry);
                if (block == null) {
                    /* No more blocks, exit */
                    break;
                }

                /* Set up the message digest context. */
                try {
                    blockDigest = MessageDigest.getInstance("md5");                    
                } catch (NoSuchAlgorithmException e) {
                    /* This should never be missing. */
                    throw new RuntimeException("Missing MD5 algorithm!", e);
                }

                /* Read blocks from the input stream until EOF is detected. */
                input = block.getInputStream();
                eof = false;
                while (!eof) {

                    /* Keep reading into byte buffer as long as it's less than 3/4ths full. */
                    nread = 0;
                    while (buffer.length - nread > (BUFFER_SIZE / 4)) {
                        int len;

                        len = input.read(buffer, nread, buffer.length - nread);

                        if (len < 0) {
                            /* Exit the loop, EOF. */
                            eof = true;
                            break;
                        }

                        nread += len;
                    }

                    /* Byte buffer full, update the digest and write it out */
                    blockDigest.update(buffer, 0, nread);
                    output.write(buffer, 0, nread);
                }

                /* EOF reached, validate the digest. We do this AFTER we've streamed out the data. */
                if (!Arrays.equals(blockDigest.digest(), block.getMD5())) {
                    throw new RemoteStreamException("S3 block " + Long.toString(blockId) + " checksum invalid.");
                }
            } catch (S3Exception e) {
                throw new RemoteStreamException("S3 failure fetching stream block " + Long.toString(blockId) +
                    ": " + e.getMessage());
            } catch (IOException e) {
                throw new RemoteStreamException("Fatal IO error handling stream block " +
                    Long.toString(blockId) + ": " + e.getMessage());
            }
        }
    }

    /**
     * Restartable block-fetching.
     * @return null if the blockId does not exist.
     */
    private S3Object fetchRemoteBlock (RemoteStream stream, Long blockId, int maxRetry)
        throws S3Exception
    {
        /* Fetch the next block, with a retry */
        S3Object object = null;
        S3RetryHandler retry = new S3RetryHandler(maxRetry);
        S3Exception retryError = null;

        do {
            /* Log the last error. */
            if (retryError != null) {
                System.err.println("S3 error fetching stream block " + Long.toString(blockId) 
                    + ", retrying: " + retryError);                    
            }

            try {
                object = _connection.getObject(_bucket, stream.streamBlockKey(blockId));
                return object; // Succeeded
            } catch (S3ServerException.NoSuchKeyException nsk) {
                /* Block doesn't exist, we're done. */
                return null;
            } catch (S3Exception s3e) {
                /* Let the retry handler check the exception */
                retryError = s3e;
                continue;
            }
        } while (retry.shouldRetry(retryError));

        // Must be unreachable
        assert(false);
        return object;
    }

    /** Maximum network buffer size (64k). */
    private static final int BUFFER_SIZE = 64 * 1024;

    /** S3 Connection. */
    private S3Connection _connection;

    /** S3 Bucket. */
    private String _bucket;
}
