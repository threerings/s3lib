//  This software code is made available "AS IS" without warranties of any
//  kind.  You may copy, display, modify and redistribute the software
//  code either by itself or as incorporated into your code; provided that
//  you do not remove any proprietary notices.  Your use of this software
//  code is at your own risk and you waive any claim against Amazon
//  Digital Services, Inc. or its affiliates with respect to your use of
//  this software code.
//
// (c) 2006-2007 Three Rings Design, Inc.

package com.threerings.s3.client;

import junit.framework.TestCase;

import org.apache.commons.codec.binary.Hex;

public class S3ObjectTest  extends TestCase
{
    /**
     * For the purpose of unit testing, validate that the two S3Objects are
     * equal. This is not a S3Object.equals() implementation, as it's not
     * possible for two S3Objects to be truly equal. Each instance is inherently
     * unique, potentially representing transient state (such as an HTTP connection)
     * that can not be replicated.
     */
    static public void testEquals (S3Object obj1, S3Object obj2, TestCase test)
        throws Exception
    {
        // Key
        test.assertEquals(obj1.getKey(), obj2.getKey());

        // Mime Type
        test.assertEquals(obj1.getMimeType(), obj2.getMimeType());

        // Checksum
        String checksum1 = new String(Hex.encodeHex(obj1.getMD5Checksum()));
        String checksum2 = new String(Hex.encodeHex(obj2.getMD5Checksum()));
        test.assertEquals(checksum1, checksum2);

        // Length
        test.assertEquals(obj1.length(), obj2.length());

        // Metadata
        test.assertTrue("Metadata does not match", obj1.getMetadata().equals(obj2.getMetadata()));
    }


    public S3ObjectTest (String name)
    {
        super(name);
    }

    public void testSomething ()
    {
        // We don't actually need to test anything right now, but junit 3.7 is
        // a big baby about TestCases with no tests.
    }
}