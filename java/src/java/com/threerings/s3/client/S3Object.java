/* 
 * S3Object vi:ts=4:sw=4:expandtab:
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

import java.util.HashMap;
import java.util.Map;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileNotFoundException;

/**
 * A representation of a single object stored in S3.
 */
public abstract class S3Object {
    /** Default binary mime type. */
    public static final String DEFAULT_MIME_TYPE = "binary/octet-stream";

    /**
     * Instantiate an S3 object with the given key.
     * @param key S3 object key.
     */
    public S3Object (String key)
    {
        this(key, DEFAULT_MIME_TYPE);
    }

    /**
     * Instantiate an S3 Object with the given key and mime type.
     * @param key S3 object key.
     * @param mimeType Object's MIME type.
     */
    public S3Object (String key, String mimeType)
    {
        this(key, mimeType, new HashMap<String,String>());
    }

    /**
     * Instantiate an S3 Object with the given key, mime type, and metadata.
     * @param key S3 object key.
     * @param mimeType Object's MIME type.
     * @param metadata Object's metadata. Metadata keys must be a single, ASCII
     *     string, and may not contain spaces. Metadata values must also be ASCII,
     *     and any leading or trailing spaces may be stripped. 
     */
    public S3Object (String key, String mimeType, Map<String,String> metadata)
    {
        _key = key;
        _mimeType = mimeType;
        _metadata = metadata;
    }

    /**
     * Returns the S3 Object Key.
     */
    public String getKey ()
    {
        return _key;
    }

    /**
     * Returns the S3 Object's MIME type.
     */
    public String getMimeType ()
    {
        return _mimeType;
    }

    /**
     * Returns the S3 Object's metadata.
     */
    public Map<String,String> getMetadata ()
    {
        return _metadata;
    }

    /**
     * Set the S3 Object's metadata.
     * Metadata keys must be a single, ASCII string, and may not contain spaces.
     */
    public void setMetadata (Map<String,String> metadata)
    {
        _metadata = metadata;
    }

    /**
     * Get the object's input stream, used to read object contents, potentially
     * from the remote S3 server. The caller is responsible for closing the
     * stream.
     */
    public abstract InputStream getInputStream () throws S3ClientException;

    /**
     * Get the object's MD5 checksum.
     */
    public abstract byte[] getMD5 () throws S3ClientException;

    /**
     * Returns the number of bytes required to store the
     * S3 Object.
     */
    public abstract long length ();
    
    /** S3 object name. */
    private String _key;
    
    /** S3 object mime-type. */
    private String _mimeType;
    
    /** S3 object meta-data. */
    private Map<String,String> _metadata;
}
