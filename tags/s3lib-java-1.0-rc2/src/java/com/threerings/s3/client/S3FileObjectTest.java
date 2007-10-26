/* 
 * S3FileObjectTest vi:ts=4:sw=4:expandtab:
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

package com.threerings.s3.client;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.codec.binary.Hex;

import org.junit.*;
import static org.junit.Assert.*;

public class S3FileObjectTest {
    @Before
    public void setUp ()
        throws Exception
    {
        _testFile = File.createTempFile("S3FileObjectTest", null);
        _fileObj = new S3FileObject("aKey", _testFile, "text/plain");        
        new FileOutputStream(_testFile).write(TEST_DATA.getBytes("utf8"));
    }
    
    @After
    public void tearDown ()
        throws Exception
    {
        _testFile.delete();
    }

    @Test
    public void testConstruct ()
        throws Exception
    {
        byte[] bytes = new byte[1024];

        int count = _fileObj.getInputStream().read(bytes);        
        assertEquals(TEST_DATA, new String(bytes, 0, count));
    }

    @Test
    public void testGetMD5Checksum ()
        throws Exception
    {
        byte[] checksum = _fileObj.getMD5();
        String hex = new String(Hex.encodeHex(checksum));
        assertEquals(TEST_DATA_MD5, hex);   
    }
    
    /** Test file. */
    protected File _testFile;

    /** Test object. */
    protected S3FileObject _fileObj;

    /** Test data. */
    protected static final String TEST_DATA = "Hello, World!";
    
    /** Pre-computed MD5 Checksum for test data. */
    protected static final String TEST_DATA_MD5 = "65a8e27d8879283831b664bd8b7f0ad4";
}