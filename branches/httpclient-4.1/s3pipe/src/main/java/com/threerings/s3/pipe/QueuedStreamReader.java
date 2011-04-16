/* 
 * QueuedStreamReader.java vi:ts=4:sw=4:expandtab:
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
 * The QueuedStreamReader buffers input from the given stream into byte[] blocks,
 * placing them in the supplied blocking queue.
 */
class QueuedStreamReader
    implements Runnable
{
    /**
     * Instantiate a new reader.
     * @param input: Input stream.
     * @param blocksize: Block size, in bytes.
     * @param queueSize: Total queue size.
     */
    public QueuedStreamReader (InputStream input, int blocksize, int queueSize) {
        _input = input;
        _blocksize = blocksize;
        _queue = new LinkedBlockingQueue<ByteBuffer>(queueSize);
    }


    // Runnable entry point.
    public void run () {
        /* Read in the stream. */
        readStream();
        _finished = true;
    }


    /**
     * Read blocks into the queue until one of the following conditions occur:
     * - The thread is interrupted
     * - EOF is reached on the InputStream
     * - An IOException is thrown.
     */
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
                    /* Save the exception and exit. */
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


    /**
     * Read a block off the queue, or return null if end-of-file has
     * been reached.
     */
    public ByteBuffer readBlock ()
        throws InterruptedException
    {
        ByteBuffer block;

        /* Loop until _finished == true and the queue is emptied. */
        while (_finished == false || (_finished == true && !_queue.isEmpty())) {
            /* Read blocks off the queue. If an error occurs reading from the stream,
             * it will not be noticed until poll() returns and _finished is
             * checked.*/
            try {
                if ((block = _queue.poll(5, TimeUnit.SECONDS)) == null) {
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


    /** Returns true if the stream reader is done reading the stream. */
    public boolean finished () {
        return _finished;
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

    /** Finished reading? */
    private boolean _finished = false;

    /** Data block size. */
    private final int _blocksize;
    
    /** Block queue. */
    private final BlockingQueue<ByteBuffer> _queue;
    
    /* Data input stream. */
    private final InputStream _input;
}
