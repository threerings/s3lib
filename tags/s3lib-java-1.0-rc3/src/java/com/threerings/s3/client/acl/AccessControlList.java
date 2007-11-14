//  This software code is made available "AS IS" without warranties of any
//  kind.  You may copy, display, modify and redistribute the software
//  code either by itself or as incorporated into your code; provided that
//  you do not remove any proprietary notices.  Your use of this software
//  code is at your own risk and you waive any claim against Amazon
//  Digital Services, Inc. or its affiliates with respect to your use of
//  this software code.
//
// (c) 2007 Three Rings Design, Inc.

package com.threerings.s3.client.acl;

/**
 * S3 Access Control List
 * TODO: To be implemented.
 */
public class AccessControlList {
    /** Standard "Canned" ACL policies. */
    public enum StandardPolicy {
        /** Owner gets full control, and anonymous principal is granted read access. */
        PUBLIC_READ("public-read"),

        /** Owner gets full control. No other entity has access */
        PRIVATE("private"),

        /** Owner gets full control, and anonymous principal is granted read and write access. */
        PUBLIC_RW("public-read-write"),

        /** Owner gets full control, and any principal authenticated Amazon S3 user is granted read access. */
        AUTHENTICATED_READ("authenticated-read");

        /**
         * Construct the standard policy with the provided S3 API string representation.
         * See http://docs.amazonwebservices.com/AmazonS3/2006-03-01/
         */
        StandardPolicy(String awsString)
        {
            _awsString = awsString;
        }

        @Override
        public String toString ()
        {
            return _awsString;
        }

        /** AWS S3 String Value. */
        private String _awsString;
    }
}