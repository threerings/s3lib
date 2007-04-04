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

import java.io.InputStream;
import java.io.IOException;

import java.nio.ByteBuffer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/*
 * Uploads streams as a series of S3Objects.
 */
public class UploadStreamer {
    /*
     * The QueuedStreamReader buffers input from the given stream into byte[] blocks,
     * placing them in the supplied blocking queue.
     */
    static private class QueuedStreamReader
        implements Runnable
    {
        public QueuedStreamReader (InputStream input, int blocksize,
            BlockingQueue<ByteBuffer> queue, Thread caller) {
            _input = input;
            _queue = queue;
            _blocksize = blocksize;
            _caller = caller;
        }


        private void readStream ()
        {
            boolean eof = false;

            while (!eof) {
                ByteBuffer block;
                byte[] backing;
                int read;
                
                block = ByteBuffer.allocate(_blocksize);
                assert(block.hasArray()); // Always true with allocate()
                backing = block.array();

                /* Read in one complete block */
                read = 0;
                while (_blocksize - read > 0) {
                    int len;

                    /* If we've been interrupted, exit the thread. */
                    if (Thread.interrupted()) {
                        return;
                    }

                    /* Read in more data. */
                    try {
                        len = _input.read(backing, read, _blocksize - read);                        
                    } catch (IOException ioe) {
                        /* Save the exception, notify our caller, and exit. */
                        _streamError = ioe;
                        return;
                    }

                    /* End of file reached? */
                    if (len < 0) {
                        /* Notify outer loop of eof. */
                        eof = true;

                        /* Break out of inner loop. */
                        break;
                    }

                    /* Add the new data to the read length and continue. */
                    read += len;
                }

                /* Block complete, add it to the queue. */
                block.limit(read);

                try {
                    _queue.put(block);                    
                } catch (InterruptedException ie) {
                    /* Exit on interrupt */
                    return;
                }
            } 
        }

        public void run () {
            /* Read in the stream. */
            readStream();
            
            /* Notify caller of completion. */
            _caller.interrupt();
        }

        /**
         * Returns the stream error, null if none.
         */
        public IOException getStreamError () {
            return _streamError;
        }

        /** If an IOException occurs, it will be saved here, and then the caller will be
         * interrupted. Object assignments are atomic, so we don't lock here. */
        private IOException _streamError = null;

        /** Caller notified on error. */
        private final Thread _caller;

        /** Data block size. */
        private final int _blocksize;
        
        /** Block queue. */
        private final BlockingQueue<ByteBuffer> _queue;
        
        /* Data input stream. */
        private final InputStream _input;
    }

    /*
     * Instantiate a new stream uploader.
     * @param input: Input stream.
     * @param blocksize: Memory block size, in bytes.
     */
    public UploadStreamer (int blocksize) {
        _blocksize = blocksize;
    }

    public void upload (InputStream input)
        throws IOException
    {
        BlockingQueue<ByteBuffer> queue;
        QueuedStreamReader reader;
        Thread readerThread;
        ByteBuffer block;   
        int retry;

        /* Buffer up to 4 blocks */
        queue = new LinkedBlockingQueue<ByteBuffer>(4);

        /* Create and start the stream reader. */
        reader = new QueuedStreamReader(input, _blocksize, queue, Thread.currentThread());
        readerThread = new Thread(reader, "Queue Stream Reader");
        readerThread.start();

        /* Read blocks off the queue. */
        while ((block = readBlock(queue)) != null) {
            // S3ByteArrayObject obj = new S3ByteArrayObject();
            System.out.println("Got block: " + block);
        }

        /* Check for error and exit */
        if (reader.getStreamError() != null) {
            throw new IOException("Failure reading input stream: " + reader.getStreamError());
        }
    }

    /**
     * Loop until we've read a block off the queue, or the thread has been interrupted.
     * Returns null if interrupted.
     */
    static private ByteBuffer readBlock (BlockingQueue<ByteBuffer> queue) {
        ByteBuffer block;

        while (!Thread.interrupted()) {
            /* Read blocks off the queue. */
            try {
                /* Wait for a block */
                if ((block = queue.poll(10, TimeUnit.SECONDS)) == null) {
                    /* Timed out, re-poll */
                    continue;                    
                } else {
                    /* Return the new block. */
                    return block;                    
                }
            } catch (InterruptedException ie) {
                /* Exit the loop. */
                break;
            }            
        }

        return null;
    }

    /** Read block size. */
    private final int _blocksize;
}
