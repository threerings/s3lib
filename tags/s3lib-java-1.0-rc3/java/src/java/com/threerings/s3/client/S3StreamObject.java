/* 
 * S3StreamObject vi:ts=4:sw=4:expandtab:
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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A representation of a (locally stream-backed) object stored in S3.
 * 
 * Object instances are "one-shot" -- that is, they can not be re-used,
 * as they will irreversibly read from the provided stream.
 */
public class S3StreamObject extends S3Object {
    /**
     * Instantiate an S3 stream object from an existing open stream.
     * 
     * @param key S3 object key.
     * @param length Stream data length.
     * @param digest MD5 digest.
     * @param input Data stream.
     */
    public S3StreamObject (String key, long length,
        byte[] digest, InputStream input)
    {
        this(key, S3Object.DEFAULT_MIME_TYPE, length, digest, input);
    }

    /**
     * Instantiate an S3 stream object from an existing open stream.
     * 
     * @param key S3 object key.
     * @param mimeType S3 object mime-type.
     * @param length Stream data length.
     * @param digest MD5 digest.
     * @param input Data stream.
     */
    public S3StreamObject (String key, String mimeType, long length,
        byte[] digest, InputStream input)
    {
        this(key, mimeType, length, digest, input, 0L);
    }

    /**
     * Instantiate an S3 stream object from an existing open stream.
     * 
     * @param key S3 object key.
     * @param mimeType S3 object mime-type.
     * @param length Stream data length.
     * @param digest MD5 digest.
     * @param input Data stream.
     * @param lastModified Last modification timestamp.
     */
    public S3StreamObject (String key, String mimeType, long length,
        byte[] digest, InputStream input, long lastModified)
    {
        this(key, mimeType, length, digest, new HashMap<String,String>(), input, lastModified);
    }
    
    /**
     * Instantiate an S3 stream object from an existing open stream.
     * 
     * @param key S3 object key.
     * @param mimeType S3 object mime-type.
     * @param length Stream data length.
     * @param digest MD5 digest.
     * @param metadata Object metadata.
     * @param input Data stream.
     * @param lastModified Last modification timestamp.
     */
    public S3StreamObject (String key, String mimeType, long length,
        byte[] digest, Map<String,String> metadata, InputStream input, long lastModified)
    {
        super(key, mimeType, metadata);
        this.length = length;
        this.digest = digest;
        this.input = input;
        this.lastModified = lastModified;
    }
     

    @Override // From S3Object
    public InputStream getInputStream ()
        throws S3ClientException
    {
        return input;
    }

    @Override // From S3Object
    public byte[] getMD5 ()
        throws S3ClientException
    {
        return digest;
    }
    
    @Override // From S3Object
    public long lastModified () {
        return lastModified;
    }

    @Override // From S3Object
    public long length () {
        return length;
    }

    /** Modification timestamp. */
    private final long lastModified;
    
    /** Data length in bytes. */
    private final long length;
    
    /** MD5 digest. */
    private final byte[] digest;

    /** Data input stream. */
    private final InputStream input;
}
