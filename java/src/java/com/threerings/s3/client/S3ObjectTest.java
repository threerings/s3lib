/* 
 * S3ObjectTest vi:ts=4:sw=4:expandtab:
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