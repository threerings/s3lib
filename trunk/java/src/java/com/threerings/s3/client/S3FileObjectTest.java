//  This software code is made available "AS IS" without warranties of any
//  kind.  You may copy, display, modify and redistribute the software
//  code either by itself or as incorporated into your code; provided that
//  you do not remove any proprietary notices.  Your use of this software
//  code is at your own risk and you waive any claim against Amazon
//  Digital Services, Inc. or its affiliates with respect to your use of
//  this software code.
//
// (c) 2006 Three Rings Design, Inc.
// (c) 2006 Amazon Digital Services, Inc. or its affiliates.

package com.threerings.s3.client;

import junit.framework.TestCase;

import java.io.File;

import org.apache.commons.codec.binary.Hex;

public class S3FileObjectTest extends TestCase
{
    public S3FileObjectTest (String name)
    {
        super(name);
    }
    
    public void setUp ()
        throws Exception
    {
        _testFile = File.createTempFile("S3FileObjectTest", null);
        _fileObj = new S3FileObject("aKey", _testFile, "text/plain");        
        _fileObj.getOutputStream().write(TEST_DATA.getBytes("utf8"));
    }
    
    public void tearDown ()
        throws Exception
    {
        _testFile.delete();
    }

    public void testConstruct ()
        throws Exception
    {
        byte[] bytes = new byte[1024];

        int count = _fileObj.getInputStream().read(bytes);        
        assertEquals(TEST_DATA, new String(bytes, 0, count));
    }

    public void testGetMD5Checksum ()
        throws Exception
    {
        byte[] checksum = _fileObj.getMD5Checksum();
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