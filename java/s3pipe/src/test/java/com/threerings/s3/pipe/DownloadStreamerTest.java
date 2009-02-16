/* 
 * DownloadStreamerTest vi:ts=4:sw=4:expandtab:
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

import com.threerings.s3.client.S3Connection;
import com.threerings.s3.client.S3TestConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;

import org.apache.commons.io.IOUtils;

import org.junit.*;
import static org.junit.Assert.*;

public class DownloadStreamerTest {

    @Before
    public void setUp ()
        throws Exception
    {
        _conn = S3TestConfig.createConnection();
        _bucket = S3TestConfig.generateTestBucketName();
        _testFile = File.createTempFile("s3pipe", "upload");
        _outputFile = File.createTempFile("s3pipe", "download");

        /* Set up our test bucket. */
        _conn.createBucket(_bucket);

        /* Populate the file with 10 bytes. We use a very small stream in
         * order to save bandwidth -- S3 requests cost money! */
        RandomAccessFile file = new RandomAccessFile(_testFile, "rw");
        file.setLength(10);
        file.close();
    }

    @After
    public void tearDown ()
        throws Exception
    {
        S3TestConfig.deleteBucket(_conn, _bucket);
        _testFile.delete();
        _outputFile.delete();
    }

    @Test
    public void testDownload ()
        throws Exception
    {
        /* Fire up an uploader with a 1 byte block size. */
        FileInputStream input = new FileInputStream(_testFile);
        UploadStreamer uploadStreamer = new UploadStreamer(_conn, _bucket, 2);
        uploadStreamer.upload("test stream", input, 5);
        
        /* Now download the stream. */
        FileOutputStream output = new FileOutputStream(_outputFile);
        DownloadStreamer downloadStreamer = new DownloadStreamer(_conn, _bucket);
        downloadStreamer.download("test stream", output, 5);

        /* Compare the input to the output. */
        assertTrue("Contents not equal!",
            IOUtils.contentEquals(
                new FileInputStream(_testFile),
                new FileInputStream(_outputFile)
            )
        );
    }

    /** Temporary test data file. */
    protected File _testFile;

    /** Output will be written here. */
    protected File _outputFile;

    /** AWS S3 Connection. */
    S3Connection _conn;

    /** Test bucket name */
    String _bucket;
}
