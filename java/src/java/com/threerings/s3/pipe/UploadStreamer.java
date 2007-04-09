/* 
 * UploadStream.java vi:ts=4:sw=4:expandtab:
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

import java.io.InputStream;
import java.io.IOException;

import java.nio.ByteBuffer;

/*
 * Uploads streams as a series of S3Objects.
 * UploadStreams are re-usable, but not thread-safe.
 *
 * @todo Implement data blocking on a sub-object level. That is, each S3Object
 *  is composed of n number of blocks. This will allow us to checksum, compress,
 *  or PKE sign smaller blocks, refetching those blocks (using HTTP range support)
 *  if necessary. This "sub-blocking" is necessary as the overhead of S3 key
 *  GET/PUT requests rules out using small S3 object blocks.
 */
class UploadStreamer {
    /*
     * Instantiate a new stream uploader.
     * S3 transactions are expensive -- the block size should be large.
     * @param connect: S3 Connection.
     * @param bucket: Destination S3 bucket.
     * @param blocksize: Upload block size, in bytes.
     */
    public UploadStreamer (S3Connection connection, String bucket,
        int blocksize)
    {
        _connection = connection;
        _bucket = bucket;
        _blocksize = blocksize;
    }


    /**
     * Upload a stream, using the given streamName.
     * @param streamName: Arbitrary stream name.
     * @param inputData: Stream to upload.
     * @param retry: Number of times to retry failed S3 operations.
     * @throws RemoteStreamException.StreamExistsException Thrown if the given stream
     *  currently exists.
     * @throws S3Exception Thrown if an S3 error occurs.
     */
    public void upload (String streamName, InputStream inputData, int maxRetry)
        throws S3Exception, RemoteStreamException
    {
        QueuedStreamReader reader;
        Thread readerThread;
        String encodedName;
        RemoteStream stream;

        /* Create and start the stream reader. */
        reader = new QueuedStreamReader(inputData, _blocksize, QUEUE_SIZE);
        readerThread = new Thread(reader, streamName + " Queue");
        readerThread.start();

        /* Instantiate a stream reference and create the stream info record */
        try {
            stream = new RemoteStream(_connection, _bucket, streamName);
            createInfoRecord(stream, maxRetry);
        } catch (S3Exception s3e) {
            throw new RemoteStreamException("S3 failure creating stream info record for '" +
                streamName + "': " + s3e.getMessage());      
        }

        /*
         * Read blocks off the queue, upload the block,
         * and increment the block ID accordingly.
         */
        try {
            ByteBuffer block;
            long blockId = 0;

            while ((block = reader.readBlock()) != null) {
                byte[] data;
                int length;
                
                /*
                 * Get access to the byte[] data.
                 */
                length = block.limit();
                if (!block.hasArray()) {
                    /* No backing array, copy the data. */
                    data = new byte[length];
                    block.get(data);
                } else {
                    /* Backing array, no copying required. */
                    data = block.array();
                }

                /*
                 * Upload the S3 Object.
                 */
                S3ByteArrayObject obj = new S3ByteArrayObject(
                    stream.streamBlockKey(blockId), data, 0, length);

                try {
                    uploadObject(obj, maxRetry);
                } catch (S3Exception s3e) {
                    System.err.println("S3 failure uploading '" +
                        obj.getKey() + "': " + s3e.getMessage());
                    readerThread.interrupt();
                    throw s3e;
                }

                /*
                 * Increment the block id.
                 */
                blockId++;
            }
        } catch (InterruptedException ie) {
            // This will exit our loop, which is sufficient.
        }

        /* Check for error and exit */
        if (reader.getStreamError() != null) {
            throw new RemoteStreamException("Failure reading input stream: " + reader.getStreamError());
        }
    }

    /**
     * Check if the stream exists, and if not, create the stream info record.
     * If the stream does exist, throw an exception -- streams are never
     * overwritten.
     */
    private void createInfoRecord (RemoteStream stream, int maxRetry)
        throws S3Exception, RemoteStreamException
    {
        S3RetryHandler retry = new S3RetryHandler(maxRetry);
        S3Exception retryError = null;

        do {
            /* Log the last error. */
            if (retryError != null) {
                System.err.println("S3 error occured creating stream info record, retrying: " +
                    retryError.getMessage());                    
            }

            try {
                /* Check if the stream exists */
                if (stream.getStreamInfo() != null) {
                    throw new RemoteStreamException.StreamExistsException("Stream \"" +
                        stream.getStreamName() + "\" exits.");
                }

                /* Create the stream info record. */
                stream.putStreamInfo();
                break;
            } catch (S3Exception s3e) {
                /* Let the retry handler check the exception */
                retryError = s3e;
                continue;
            }
        } while (retry.shouldRetry(retryError));
    }


    /**
     * Upload the S3 object, with a simple retry.
     */
    private void uploadObject (S3Object object, int maxRetry)
        throws S3Exception
    {
        S3RetryHandler retry = new S3RetryHandler(maxRetry);
        S3Exception retryError = null;

        do {
            /* Log the last error. */
            if (retryError != null) {
                System.err.println("S3 failure uploading '" + object.getKey() + "', retrying: " + retryError);                   
            }

            try {
                _connection.putObject(_bucket, object, AccessControlList.StandardPolicy.PRIVATE);                            
            } catch (S3Exception e) {
                /* Let the retry handler check the exception */
                retryError = e;
                continue;
            }
            break;
        } while (retry.shouldRetry(retryError));
    }


    /** Queue size. */
    private final int QUEUE_SIZE = 4;

    /** S3 Connection. */
    private S3Connection _connection;

    /** S3 Bucket. */
    private String _bucket;

    /** Read block size. */
    private final int _blocksize;
}
