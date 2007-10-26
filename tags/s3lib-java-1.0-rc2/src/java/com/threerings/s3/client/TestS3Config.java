package com.threerings.s3.client;

import java.io.IOException;
import java.security.SecureRandom;

/**
 * S3 unit test configuration and utilities.
 */
public class TestS3Config {
    /**
     * Return the test-supplied AWS id
     */
    public static String getId () {
        return System.getProperty("aws.id");
    }
    
    /**
     * Return the test-supplied AWS key
     */
    public static String getKey () {
        return System.getProperty("aws.key");
    }

    /**
     * Create a new S3 connection using the test
     * AWS key and id.
     */
    public static S3Connection createConnection ()
        throws S3Exception
    {
        String id = getId();
        String key = getKey();
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
     * Generate a unique test bucket name.
     */
    public static String generateTestBucketName ()
    {
        int random = new SecureRandom().nextInt(Integer.MAX_VALUE);
        return  "test-" + getId() + "-" + random;
    }

}
