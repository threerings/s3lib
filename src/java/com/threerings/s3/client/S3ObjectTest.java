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

import org.apache.commons.codec.binary.Hex;

import org.junit.*;
import static org.junit.Assert.*;

public class S3ObjectTest {
    /**
     * A unit test utility function to validate that the two S3Objects are
     * equal. This is not a S3Object.equals() implementation, as it's not
     * possible for two S3Objects to be truly equal. Each instance is inherently
     * unique, potentially representing transient state (such as an HTTP connection)
     * that can not be replicated.
     */
    static public void testEquals (S3Object obj1, S3Object obj2)
        throws Exception
    {
        // Key
        assertEquals(obj1.getKey(), obj2.getKey());

        // Mime Type
        assertEquals(obj1.getMimeType(), obj2.getMimeType());

        // Checksum
        String checksum1 = new String(Hex.encodeHex(obj1.getMD5()));
        String checksum2 = new String(Hex.encodeHex(obj2.getMD5()));
        assertEquals(checksum1, checksum2);

        // Length
        assertEquals(obj1.length(), obj2.length());

        // Metadata
        assertTrue("Metadata does not match", obj1.getMetadata().equals(obj2.getMetadata()));
    }
    
    /**
     * Check that the last modified date is set.
     */
    @Test
    public void testLastModified () {
        S3Object object = new S3ByteArrayObject("test", new byte[0], "text/plain");
        assertEquals(0L, object.lastModified());
    }
}