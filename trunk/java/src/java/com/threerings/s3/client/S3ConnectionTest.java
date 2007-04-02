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

import com.threerings.s3.client.acl.AccessControlList;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.codec.binary.Hex;

public class S3ConnectionTest extends TestCase
{
    public S3ConnectionTest (String name)
    {
        super(name);
    }
    
    public void setUp ()
        throws Exception
    {
        FileOutputStream fileOutput;

        _awsId = System.getProperty("aws.id");
        _awsKey = System.getProperty("aws.key");
        _conn = new S3Connection(_awsId, _awsKey);
        _testBucketName = "test-" + _awsId;
        _testFile = File.createTempFile("S3FileObjectTest", null);

        // Create a file object
        fileOutput = new FileOutputStream(_testFile);
        fileOutput.write(TEST_DATA.getBytes("utf8"));
        fileOutput.close();
        _fileObj = new S3FileObject("aKey", _testFile);
        
        // Create the test bucket
        _conn.createBucket(_testBucketName, null);
    }

    public void tearDown ()
        throws Exception
    {
        /* Delete all objects in the test bucket. */
        S3ObjectListing listing = _conn.listObjects(_testBucketName);
        for (S3ObjectEntry entry : listing.getEntries()) {
            _conn.deleteObject(_testBucketName, entry.getKey());
        }

        /* Delete the test bucket. */
        _conn.deleteBucket(_testBucketName, null);
        _testFile.delete();
    }

    public void testCreateBucket ()
        throws Exception
    {
        // No exception, all is well.
        _conn.createBucket(_testBucketName + "testCreateBucket", null);
        _conn.deleteBucket(_testBucketName + "testCreateBucket", null);
    }


    /**
     * Test listBucket.
     * @TODO: Move of these tests to a S3ObjectListingTest class.
     */
    public void testListBucket ()
        throws Exception
    {
        S3ObjectListing listing;
        List<S3ObjectEntry> entries;
        S3ObjectEntry entry;
        S3Owner owner;

        // Send an object to the mother ship
        _conn.putObject(_testBucketName, _fileObj, AccessControlList.StandardPolicy.PRIVATE);

        listing = _conn.listObjects(_testBucketName);
        entries = listing.getEntries();

        /* Validate the bucket name */
        assertEquals(_testBucketName, listing.getBucketName());

        /* Validate the entries. There should only be one. */
        assertEquals(1, entries.size());
        entry = entries.get(0);

        /* Key name. */
        assertEquals(_fileObj.getKey(), entry.getKey());

        /* Last modified -- should be within the last 5 minutes. (5 minutes * 60 seconds * 1000 milliseconds)*/
        assertTrue(
            "Object's last modified date is not within the last 5 minutes: " + entry.getLastModified(),
            (new Date().getTime() - entry.getLastModified().getTime()) < 5 * 60 * 1000
        );

        /* ETag (MD5) */
        String md5 = new String(Hex.encodeHex(_fileObj.getMD5Checksum()));
        assertEquals(md5, entry.getETag());

        /* Size */
        assertEquals(_testFile.length(), entry.getSize());

        /* Storage Class */
        assertEquals(STORAGE_CLASS, entry.getStorageClass());
        
        /* 
         * Test the object's owner, too.
         * We can't really test much here, besides that it has one.
         */
        owner = entry.getOwner();
        assertNotNull(owner.getId());
        assertNotNull(owner.getDisplayName());
    }


    public void testPutObject ()
        throws Exception
    {
        // Send it to the mother ship
        _conn.putObject(_testBucketName, _fileObj, AccessControlList.StandardPolicy.PRIVATE);
    }


    public void testGetObject ()
        throws Exception
    {
        S3Object remote;

        // Send it to the mother ship
        _conn.putObject(_testBucketName, _fileObj, AccessControlList.StandardPolicy.PRIVATE);

        // Fetch it back out again
        S3Object obj = _conn.getObject(_testBucketName, _fileObj.getKey());

        // Ensure that it is equal to the object we uploaded
        S3ObjectTest.testEquals(_fileObj, obj, this);

        // Validate the object file data, too.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        InputStream input = obj.getInputStream();
        byte[] data = new byte[1024];
        int nread;

        while ((nread = input.read(data)) > 0) {
            output.write(data, 0, nread);

            // Sanity check. We didn't upload more than 2 megs!
            if (output.size() > 2048) {
                break;
            }
        }

        input.close();
        assertEquals(TEST_DATA, output.toString("utf8"));
    }

    public void testObjectMetadata ()
        throws Exception
    {
        HashMap<String,String> metadata;
        
        // Some test data
        metadata = new HashMap<String,String>();
        metadata.put("meta", "value whitespace");
        _fileObj.setMetadata(metadata);

        // Send it to the mother ship
        _conn.putObject(_testBucketName, _fileObj, AccessControlList.StandardPolicy.PRIVATE);

        // Fetch it back out again and validate the metadata
        S3Object obj = _conn.getObject(_testBucketName, _fileObj.getKey());

        // Ensure that it is equal to the object we uploaded
        S3ObjectTest.testEquals(_fileObj, obj, this);
    }

    public void testErrorHandling ()
        throws Exception
    {
        S3Connection badConn = new S3Connection(_awsId, "bad key");
        try {
            badConn.createBucket(_testBucketName, null);
            fail("Did not throw S3SignatureDoesNotMatchException");            
        } catch (S3ServerException.SignatureDoesNotMatchException e) {
            // Do nothing
        }
    }
    
    /** Amazon S3 Authenticated Connection */
    private S3Connection _conn;
    
    /** Amazon Web Services ID */
    protected String _awsId;
    
    /** Amazon Web Services Key */
    protected String _awsKey;
    
    /** Test bucket */
    protected String _testBucketName;
    
    /** Test file. */
    protected File _testFile;
    
    /** Test object. */
    protected S3FileObject _fileObj;

    /** Test data. */
    protected static final String TEST_DATA = "Hello, World!";

    /** Standard S3 storage class. */
    protected static final String STORAGE_CLASS = "STANDARD";
}