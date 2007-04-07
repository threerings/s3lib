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

import java.nio.ByteBuffer;

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
    public void download (String streamName, OutputStream output, int retry)
        throws S3Exception, RemoteStreamException
    {
        RemoteStream stream;
        RemoteStreamInfo info;

        stream = new RemoteStream(_connection, _bucket, streamName);

        /* Fetch the stream info record.
         * The caller must handle any remote stream exceptions. We only
         * retry on transient S3 exceptions. */
        for (int i = 0; i < retry; i++) {
            try {
                /* Fetch the stream info */
                info = stream.getStreamInfo();
                if (info == null) {
                    throw new RemoteStreamException.NoSuchStreamException("Stream \"" + streamName + "\" does not exist.");
                }
    
            } catch (S3Exception s3e) {
                /* Transient error occured. If the next loop will hit the maximum
                 * retry count, throw an exception. Otherwise, log an
                 * error */
                if (i < retry - 1) {
                    System.err.println("S3 Failure fetching stream info record for '" +
                        streamName + "': " + s3e.getMessage());
                } else {
                    throw s3e;                            
                }
            }
        }


        /*
         * Download the blocks from S3.
         */
        boolean finished = false;
        for (long blockId = 0; !finished; blockId++) {
            /* Fetch the next block, with a retry */
            for (int i = 0; i < retry; i++) {
                try {
                    S3Object object = _connection.getObject(_bucket, stream.streamBlockKey(blockId));
                    break; // Succeeded, exit the retry loop.
                } catch (S3ServerException.NoSuchKeyException nsk) {
                    /* Block doesn't exist, we're done. */
                    finished = true;
                } catch (S3Exception s3e) {
                    /* Error occured. If the next loop will hit the maximum
                     * retry count, throw an exception. Otherwise, log an
                     * error */
                    if (i < retry - 1) {
                        System.err.println("S3 Failure downloading " + stream.streamBlockKey(blockId) + ": " + s3e.getMessage());
                    } else {
                        throw s3e;                            
                    }
                }
            }
            
            /* Block fetched, stream in the data. */
            
        }


    }

    /** S3 Connection. */
    private S3Connection _connection;

    /** S3 Bucket. */
    private String _bucket;
}
