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
import com.threerings.s3.client.S3ObjectListing;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Most of these tests are hardwired to fail if encoding/decoding routines are
 * changed, as any changes will result in lost access to old data.
 */
public class RemoteStreamTest {

    @Before
    public void setUp ()
        throws Exception
    {
        _conn = S3TestConfig.createConnection();
        _bucket = S3TestConfig.generateTestBucketName();
        _stream = new RemoteStream(_conn, _bucket, STREAM_NAME);

        /* Set up our test bucket. */
        _conn.createBucket(_bucket);
    }

    @After
    public void tearDown ()
        throws Exception
    {
        S3TestConfig.deleteBucket(_conn, _bucket);
    }

    @Test
    public void testGetAllStreams ()
        throws Exception
    {
        List<RemoteStreamInfo> streams;

        _stream.putStreamInfo();
        streams = RemoteStream.getAllStreams(_conn, _bucket);
        assertEquals(1, streams.size());
        assertEquals(STREAM_NAME, streams.get(0).getName());
    }

    @Test
    public void testPutStreamInfo ()
        throws Exception
    {
        _stream.putStreamInfo();
    }

    @Test
    public void testGetStreamInfoRecord ()
        throws Exception
    {
        RemoteStreamInfo info;

        /* No info record. */
        assertNull(_stream.getStreamInfo());

        /* With an info record. */
        _stream.putStreamInfo();
        info = _stream.getStreamInfo();
        assertEquals(STREAM_NAME, info.getName());
        assertEquals(RemoteStream.VERSION, info.getVersion());
        
        /* Last modified -- should be within the last 5 minutes. (5 minutes * 60 seconds * 1000 milliseconds)*/
        assertTrue(
            "Streams's last modified date is not within the last 5 minutes: " + info.getCreationDate(),
            (new Date().getTime() - info.getCreationDate().getTime()) < 5 * 60 * 1000
        );
    }

    @Test
    public void testDeleteStream ()
        throws Exception
    {
        /* Fire up an uploader with a 1 byte block size. */
        InputStream input = new ByteArrayInputStream(new byte[10]);
        UploadStreamer streamer = new UploadStreamer(_conn, _bucket, 2);
        streamer.upload(STREAM_NAME, input, 5);
    
        RemoteStream stream = new RemoteStream(_conn, _bucket, STREAM_NAME);
        stream.putStreamInfo();
        stream.delete(5);
        S3ObjectListing listing = _conn.listObjects(_bucket);
        assertEquals(0, listing.getEntries().size());
    }

    @Test
    public void testStreamInfoKey () {
        assertEquals("stream." + ENCODED_STREAM_NAME + ".info", _stream.streamInfoKey());
    }

    @Test
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
