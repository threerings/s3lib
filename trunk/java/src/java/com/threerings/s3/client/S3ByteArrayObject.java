/* 
 * S3ByteArrayObject vi:ts=4:sw=4:expandtab:
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

import org.apache.commons.codec.digest.DigestUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.List;
import java.util.Map;

/**
 * A representation of a array-backed object stored in S3.
 */
public class S3ByteArrayObject extends S3Object {
    
    /**
     * Instantiate an S3 byte object with the given key and data.
     * The data is not copied, and a reference is retained.
     *
     * @param key S3 object key.
     * @param data Object data.
     */
    public S3ByteArrayObject(String key, byte[] data) {
        this(key, data, S3Object.DEFAULT_MIME_TYPE);
    }

    public S3ByteArrayObject(String key, byte[] data, String mimeType) {
        this(key, data, 0, data.length, mimeType);
    }

    public S3ByteArrayObject(String key, byte[] data, int offset, int length) {
        this(key, data, offset, length, S3Object.DEFAULT_MIME_TYPE);
    }

    /**
     * Instantiate an S3 byte object.
     * The data is not copied, and a reference is retained.
     *
     * @param key S3 object key.
     * @param data Object data;
     * @param mimeType Object's MIME type.
     */
    public S3ByteArrayObject(String key, byte[] data, int offset, int length, String mimeType)
    {
        super(key, mimeType);
        
        MessageDigest md;

        _data = data;
        _offset = offset;
        _length = length;

        // Compute the _md5 digest
        try {
            md = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException nsa) {
            // If MD5 isn't available, we're in trouble.
            throw new RuntimeException(nsa);
        }

        md.update(_data, _offset, _length);
        _md5 =  md.digest();
    }

    @Override // From S3Object
    public InputStream getInputStream ()
    {
        return new ByteArrayInputStream(_data, _offset, _length);
    }

    @Override // From S3Object
    public byte[] getMD5 ()
    {
        return _md5;
    }

    @Override // From S3Object
    public long length () {
        return _length;
    }

    /** Backing byte array. */
    private byte[] _data;
    
    /** Data length. */
    private int _length;
    
    /** Data offset. */
    private int _offset;

    /** MD5 Digest. */
    private byte[] _md5;
}
