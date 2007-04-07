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

import java.io.InputStream;
import java.io.IOException;

import java.nio.ByteBuffer;

/*
 * Uploads streams as a series of S3Objects.
 * UploadStreams are re-usable, but not thread-safe.
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
    public void upload (String streamName, InputStream inputData, int retry)
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

        /* Instantiate a stream reference, with a retry block */
        stream = new RemoteStream(_connection, _bucket, streamName);

        for (int i = 0; i < retry; i++) {
            /* The caller must handle any remote stream exceptions. */
            try {
                /* Check if the stream exists */
                if (stream.getStreamInfo() != null) {
                    throw new RemoteStreamException.StreamExistsException("Stream \"" + streamName + "\" exits.");
                }
                
                /* Create the stream info record. */
                stream.putStreamInfo();
                
                /* Success, exit the retry loop. */
                break;
            } catch (S3Exception s3e) {
                /* Transient error occured. If the next loop will hit the maximum
                 * retry count, throw an exception. Otherwise, log an
                 * error */
                if (i < retry - 1) {
                    System.err.println("S3 Failure creating stream info record for '" +
                        streamName + "': " + s3e.getMessage());
                } else {
                    throw s3e;                            
                }
            }
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

                for (int i = 0; i < retry; i++) {
                    try {
                        _connection.putObject(_bucket, obj, AccessControlList.StandardPolicy.PRIVATE);
                        break; // Succeeded, exit the retry loop.
                    } catch (S3Exception s3e) {
                        /* Error occured. If the next loop will hit the maximum
                         * retry count, throw an exception. Otherwise, log an
                         * error */
                        if (i < retry - 1) {
                            System.err.println("S3 Failure uploading " +
                                obj.getKey() + ": " + s3e.getMessage());
                        } else {
                            readerThread.interrupt();
                            throw s3e;                            
                        }
                    }
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

    /** Queue size. */
    private final int QUEUE_SIZE = 4;

    /** S3 Connection. */
    private S3Connection _connection;

    /** S3 Bucket. */
    private String _bucket;

    /** Read block size. */
    private final int _blocksize;
}
