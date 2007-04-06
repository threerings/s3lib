/* 
 * RemoteStreamTest.java vi:ts=4:sw=4:expandtab:
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
import com.threerings.s3.client.S3ConnectionTest;

import junit.framework.TestCase;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;

/**
 * Most of these tests are hardwired to fail if encoding/decoding routines are
 * changed, as any changes will result in lost access to old data.
 */
public class RemoteStreamTest extends TestCase
{
    public RemoteStreamTest (String name) {
        super(name);
    }

    public void setUp ()
        throws Exception
    {
        _conn = S3ConnectionTest.createConnection();
        _bucket = S3ConnectionTest.generateTestBucketName();
        _stream = new RemoteStream(_conn, _bucket, STREAM_NAME);

        /* Set up our test bucket. */
        _conn.createBucket(_bucket, null);
    }

    public void tearDown ()
        throws Exception
    {
        S3ConnectionTest.deleteBucket(_conn, _bucket);
    }

    public void testPutStreamInfo ()
        throws Exception
    {
        _stream.putStreamInfo();
    }

    public void testGetStreamInfoRecord ()
        throws Exception
    {
        RemoteStreamInfo info;

        try {
            info = _stream.getStreamInfo();
            fail("getStreamInfo() on a nonexistent stream did not throw a StreamNotFoundException");
        } catch (RemoteStreamException.StreamNotFoundException snf) {
            // Expected
        }

        _stream.putStreamInfo();
        info = _stream.getStreamInfo();
        assertEquals(RemoteStream.VERSION, info.getVersion());
    }

    public void testExists ()
        throws Exception
    {
        assertTrue(!_stream.exists());
        _stream.putStreamInfo();
        assertTrue(_stream.exists());
    }

    public void testStreamInfoKey () {
        assertEquals("stream." + ENCODED_STREAM_NAME + ".info", _stream.streamInfoKey());
    }

    public void testStreamBlockKey () {
        assertEquals("stream." + ENCODED_STREAM_NAME + ".block.0", _stream.streamBlockKey(0));
    }

    /** Test stream. */
    private RemoteStream _stream;

    /** AWS S3 Connection. */
    S3Connection _conn;

    /** Test bucket name */
    String _bucket;

    /** Test stream name. */
    private static final String STREAM_NAME = "aStreamName";
    
    /** Encoded (base64) test stream name. */
    private static final String ENCODED_STREAM_NAME;
    
    /* Base64 encode the stream name. */
    static {
        try {
            Base64 encoder = new Base64();
            byte[] data = STREAM_NAME.getBytes("utf-8");
            ENCODED_STREAM_NAME = new String(encoder.encode(data), "ascii");            
        } catch (UnsupportedEncodingException uee) {
            // utf-8 and ascii must always be available.
            throw new RuntimeException("Missing a standard encoding", uee);
        }
    }    
}
