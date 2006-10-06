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

import org.apache.commons.httpclient.protocol.Protocol;

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
        // No exception, all is well.
        _conn.createBucket(_testBucketName, null);
        _conn.deleteBucket(_testBucketName, null);
    }
    
    public void testErrorHandling ()
        throws Exception
    {
        AWSAuthConnection badConn = new AWSAuthConnection(_awsId, "bad key");
        try {
            badConn.createBucket(_testBucketName, null);
            fail("Did not throw S3SignatureDoesNotMatchException");            
        } catch (S3Exception.SignatureDoesNotMatchException e) {
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
}