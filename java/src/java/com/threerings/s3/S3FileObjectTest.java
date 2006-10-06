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

package com.threerings.s3;

import junit.framework.TestCase;

import java.io.File;

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

        S3FileObject fileObj = new S3FileObject("aKey", _testFile);        
        fileObj.getOutputStream().write(TEST_DATA.getBytes());
        int count = fileObj.getInputStream().read(bytes);        
        assertTrue(new String(bytes, 0, count).equals(TEST_DATA));
    }
    
    /** Test file. */
    protected File _testFile;
    
    /** Test data. */
    protected static final String TEST_DATA = "Hello, World!";
}