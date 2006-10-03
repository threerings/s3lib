//  This software code is made available "AS IS" without warranties of any
//  kind.  You may copy, display, modify and redistribute the software
//  code either by itself or as incorporated into your code; provided that
//  you do not remove any proprietary notices.  Your use of this software
//  code is at your own risk and you waive any claim against Amazon
//  Digital Services, Inc. or its affiliates with respect to your use of
//  this software code. (c) 2006 Amazon Digital Services, Inc. or its
//  affiliates.

import java.util.Map;
import java.util.TreeMap;
import java.util.Arrays;

import com.amazon.s3.AWSAuthConnection;
import com.amazon.s3.QueryStringAuthGenerator;
import com.amazon.s3.S3Object;

public class S3Driver {

    static final String awsAccessKeyId = "<INSERT YOUR AWS ACCESS KEY ID HERE>";
    static final String awsSecretAccessKey = "<INSERT YOUR AWS SECRET ACCESS KEY HERE>";

    static final String bucketName = awsAccessKeyId + "-test-bucket";
    static final String keyName = "test-key";

    public static void main(String args[]) throws Exception {
        if (awsAccessKeyId.startsWith("<INSERT")) {
            System.err.println("Please examine S3Driver.java and update it with your credentials");
            System.exit(-1);
        }

        AWSAuthConnection conn =
            new AWSAuthConnection(awsAccessKeyId, awsSecretAccessKey);

        System.out.println("----- creating bucket -----");
        System.out.println(conn.createBucket(bucketName, null).connection.getResponseMessage());

        System.out.println("----- listing bucket -----");
        System.out.println(conn.listBucket(bucketName, null, null, null, null).entries);

        System.out.println("----- putting object -----");
        S3Object object = new S3Object("this is a test".getBytes(), null);
        Map headers = new TreeMap();
        headers.put("Content-Type", Arrays.asList(new String[] { "text/plain" }));
        System.out.println(
                conn.put(bucketName, keyName, object, headers).connection.getResponseMessage()
            );

        System.out.println("----- listing bucket -----");
        System.out.println(conn.listBucket(bucketName, null, null, null, null).entries);

        System.out.println("----- getting object -----");
        System.out.println(
                new String(conn.get(bucketName, keyName, null).object.data)
            );

        System.out.println("----- query string auth example -----");
        QueryStringAuthGenerator generator =
            new QueryStringAuthGenerator(awsAccessKeyId, awsSecretAccessKey, true);
        generator.setExpiresIn(60 * 1000);

        System.out.println("Try this url in your web browser (it will only work for 60 seconds)\n");
        System.out.println(generator.get(bucketName, keyName, null));
        System.out.print("\npress enter> ");
        System.in.read();

        System.out.println("\nNow try just the url without the query string arguments.  It should fail.\n");
        System.out.println(generator.makeBareURL(bucketName, keyName));
        System.out.print("\npress enter> ");
        System.in.read();

        System.out.println("----- putting object with metadata and public read acl -----");

        Map metadata = new TreeMap();
        metadata.put("blah", Arrays.asList(new String[] { "foo" }));
        object = new S3Object("this is a publicly readable test".getBytes(), metadata);

        headers = new TreeMap();
        headers.put("x-amz-acl", Arrays.asList(new String[] { "public-read" }));
        headers.put("Content-Type", Arrays.asList(new String[] { "text/plain" }));

        System.out.println(
                conn.put(bucketName, keyName + "-public", object, headers).connection.getResponseMessage()
            );

        System.out.println("----- anonymous read test -----");
        System.out.println("\nYou should be able to try this in your browser\n");
        System.out.println(generator.makeBareURL(bucketName, keyName + "-public"));
        System.out.print("\npress enter> ");
        System.in.read();

        System.out.println("----- getting object's acl -----");
        System.out.println(new String(conn.getACL(bucketName, keyName, null).object.data));

        System.out.println("----- deleting objects -----");
        System.out.println(
                conn.delete(bucketName, keyName, null).connection.getResponseMessage()
            );
        System.out.println(
                conn.delete(bucketName, keyName + "-public", null).connection.getResponseMessage()
            );

        System.out.println("----- listing bucket -----");
        System.out.println(conn.listBucket(bucketName, null, null, null, null).entries);

        System.out.println("----- listing all my buckets -----");
        System.out.println(conn.listAllMyBuckets(null).entries);

        System.out.println("----- deleting bucket -----");
        System.out.println(
                conn.deleteBucket(bucketName, null).connection.getResponseMessage()
            );
    }
}
