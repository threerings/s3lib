/* 
 * QueuedStreamReaderTest.java vi:ts=4:sw=4:expandtab:
 *
 * Copyright (c) 2005 - 2007 Three Rings Design, Inc.
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

import junit.framework.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileOutputStream;

import java.nio.ByteBuffer;

import java.util.Arrays;

public class QueuedStreamReaderTest extends TestCase
{
    public QueuedStreamReaderTest (String name) {
        super(name);
    }

    public void setUp ()
        throws Exception
    {
        _testFile = File.createTempFile("s3pipe", "data");
        
        /* Populate the file with 100K of data */
        FileOutputStream output = new FileOutputStream(_testFile);
        int written = 0;
        while (written < 100 * 1024) {
            output.write(TEST_DATA);
            written += TEST_DATA.length;
        }
        output.flush();
        output.close();
    }

    public void tearDown () {
        _testFile.delete();
    }

    public void testSomething ()
        throws Exception
    {
        QueuedStreamReader reader;
        InputStream data;
        Thread readerThread;
        ByteBuffer block;

        data = new FileInputStream(_testFile);

        /* Create and start the stream reader. */
        reader = new QueuedStreamReader(new FileInputStream(_testFile), BLOCK_SIZE, 4);
        readerThread = new Thread(reader);
        readerThread.start();

        /* Read blocks off the queue. */
        try {
            byte[] comparable = new byte[BLOCK_SIZE];
            while ((block = reader.readBlock()) != null) {
                block.get(comparable, 0, block.limit());
                assertTrue("Block doesn't match test data.",
                    Arrays.equals(TEST_DATA, comparable));
            }
        } catch (InterruptedException ie) {
            // Something here
        }

        /* Check for error and exit */
        if (reader.getStreamError() != null) {
            throw new IOException("Failure reading input stream: " + reader.getStreamError());
        }
    }

    /** Temporary test data file. */
    protected File _testFile;

    /** Block data. */
    static protected final byte[] TEST_DATA;

    /** Block size. */
    static protected final int BLOCK_SIZE;
    
    static {
        try {
            TEST_DATA = "S3 Test Data".getBytes("UTF8");            
        } catch (Exception e) {
            throw new RuntimeException("Missing utf8 encoding!");
        }
        BLOCK_SIZE = TEST_DATA.length;
    }
}
