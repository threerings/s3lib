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
import java.io.FileOutputStream;

public class AWSAuthConnectionTest extends TestCase
{
    public AWSAuthConnectionTest (String name)
    {
        super(name);
    }
    
    public void setUp ()
        throws Exception
    {
        _awsId = System.getProperty("aws.id");
        _awsKey = System.getProperty("aws.key");
        _conn = new AWSAuthConnection(_awsId, _awsKey);
        _testBucketName = "test-" + _awsId;
        _testFile = File.createTempFile("S3FileObjectTest", null);
    }
    
    public void tearDown ()
    {
        _testFile.delete();
    }

    public void testCreateBucket ()
        throws Exception
    {
        // No exception, all is well.
        _conn.createBucket(_testBucketName, null);
        _conn.deleteBucket(_testBucketName, null);
    }
    
    public void testPutObject ()
        throws Exception
    {
        // Create a file object
        FileOutputStream fileOutput = new FileOutputStream(_testFile);
        S3FileObject fileObj = new S3FileObject("aKey", _testFile);     
        fileOutput.write(TEST_DATA.getBytes());
     
        // Create a bucket to stuff it in
        _conn.createBucket(_testBucketName, null);
        
        try {
            // Send it to the mother ship
            _conn.putObject(_testBucketName, fileObj);
        
            // Hey, you can't have that!
            _conn.deleteObject(_testBucketName, fileObj);
        } finally {
            _conn.deleteBucket(_testBucketName, null);
        }
    }
    
    
    public void testErrorHandling ()
        throws Exception
    {
        AWSAuthConnection badConn = new AWSAuthConnection(_awsId, "bad key");
        try {
            badConn.createBucket(_testBucketName, null);
            fail("Did not throw S3SignatureDoesNotMatchException");            
        } catch (S3ServerException.SignatureDoesNotMatchException e) {
            // Do nothing
        }
    }
    
    /** Amazon S3 Authenticated Connection */
    protected AWSAuthConnection _conn;
    
    /** Amazon Web Services ID */
    protected String _awsId;
    
    /** Amazon Web Services Key */
    protected String _awsKey;
    
    /** Test bucket */
    protected String _testBucketName;
    
    /** Test file. */
    protected File _testFile;
    
    /** Test data. */
    protected static final String TEST_DATA = "Hello, World!";
}