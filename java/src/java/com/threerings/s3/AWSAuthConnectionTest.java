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

public class AWSAuthConnectionTest extends TestCase
{
    public AWSAuthConnectionTest (String name)
    {
        super(name);
    }
    
    public void setUp ()
    {
        _awsId = System.getProperty("aws.id");
        _awsKey = System.getProperty("aws.key");
        _conn = new AWSAuthConnection(_awsId, _awsKey);
        _testBucketName = "test-" + _awsId;
    }

    public void testCreateBucket ()
        throws Exception
    {
        // Response response;
        
        _conn.createBucket(_testBucketName, null);
        /*
        assertEquals("Couldn't create bucket: " +
            response.connection.getResponseMessage() + ".",
            HttpURLConnection.HTTP_OK,
            response.connection.getResponseCode());
        */
        
        _conn.deleteBucket(_testBucketName, null);
        /*
        assertEquals("Couldn't delete bucket: " +
            response.connection.getResponseMessage() + ".",
            HttpURLConnection.HTTP_NO_CONTENT,
            response.connection.getResponseCode());
        */
    }
    
    /** Amazon S3 Authenticated Connection */
    protected AWSAuthConnection _conn;
    
    /** Amazon Web Services ID */
    protected String _awsId;
    
    /** Amazon Web Services Key */
    protected String _awsKey;
    
    /** Test bucket */
    protected String _testBucketName;
}