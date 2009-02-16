package com.threerings.s3.pipe;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Properties;

import java.io.InputStream;
import java.io.IOException;

import com.threerings.s3.client.S3Connection;
import com.threerings.s3.client.S3ObjectListing;
import com.threerings.s3.client.S3ObjectEntry;
import com.threerings.s3.client.S3Exception;

/**
 * S3 unit test configuration and utilities.
 */
public class S3TestConfig {
    private static final Properties props;
    static {
        props = new Properties();
        
        try {
            InputStream stream = S3TestConfig.class.getResourceAsStream("/test.properties");
            if (stream == null)
                throw new RuntimeException("Missing test preferences: test.properties");

            props.load(stream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties", e);
        }
    }
    
    /**
     * Return the test-supplied AWS id
     */
    public static String getId () {
        return props.getProperty("aws.id");
    }
    
    /**
     * Return the test-supplied AWS key
     */
    public static String getKey () {
        return props.getProperty("aws.key");
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
