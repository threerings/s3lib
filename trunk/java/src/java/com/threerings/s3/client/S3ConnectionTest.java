/* 
 * S3ConnectionTest vi:ts=4:sw=4:expandtab:
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

import com.threerings.s3.client.acl.AccessControlList;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.codec.binary.Hex;

public class S3ConnectionTest extends TestCase
{
    /**
     * Return the test-supplied AWS id
     */
    public static String getId ()
    {
        return System.getProperty("aws.id");
    }

    /**
     * Create a new S3 connection using the test
     * AWS key and id.
     */
    public static S3Connection createConnection ()
        throws S3Exception
    {
        String id = getId();
        String key = System.getProperty("aws.key");
        return new S3Connection(id, key);        
    }

    /**
     * Recursively delete a bucket and all of its keys.
     */
    public static void deleteBucket (S3Connection conn, String bucketName)
        throws S3Exception, IOException
    {
        /* Delete all objects in the bucket. */
        S3ObjectListing listing = conn.listObjects(bucketName);
        for (S3ObjectEntry entry : listing.getEntries()) {
            conn.deleteObject(bucketName, entry.getKey());
        }

        /* Delete the bucket. */
        conn.deleteBucket(bucketName);
    }

    /**
     * Generate a unique-ish test bucket name.
     */
    public static String generateTestBucketName ()
    {
        return  "test-" + getId();
    }

    public S3ConnectionTest (String name)
    {
        super(name);
    }
    
    public void setUp ()
        throws Exception
    {
        FileOutputStream fileOutput;

        _conn = createConnection();
        _testBucketName = generateTestBucketName();
        _testFile = File.createTempFile("S3FileObjectTest", null);

        // Create a file object
        fileOutput = new FileOutputStream(_testFile);
        fileOutput.write(TEST_DATA.getBytes("utf8"));
        fileOutput.close();
        _fileObj = new S3FileObject("aKey", _testFile);
        
        // Create the test bucket
        _conn.createBucket(_testBucketName);
    }

    public void tearDown ()
        throws Exception
    {
        deleteBucket(_conn, _testBucketName);
        _testFile.delete();
    }

    public void testCreateBucket ()
        throws Exception
    {
        // No exception, all is well.
        _conn.createBucket(_testBucketName + "testCreateBucket");
        _conn.deleteBucket(_testBucketName + "testCreateBucket");
    }


    /**
     * Test listBucket.
     * Detailed testing of the response parsing, using static test data, is
     * also done in the S3ObjectListingHandler unit tests.
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
        S3Connection badConn = new S3Connection(getId(), "bad key");
        try {
            badConn.createBucket(_testBucketName);
            fail("Did not throw S3SignatureDoesNotMatchException");            
        } catch (S3ServerException.SignatureDoesNotMatchException e) {
            // Do nothing
        }
    }
    
    /** Amazon S3 Authenticated Connection */
    private S3Connection _conn;
    
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