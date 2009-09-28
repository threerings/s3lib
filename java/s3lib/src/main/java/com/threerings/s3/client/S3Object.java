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

/**
 * A representation of a single object stored in S3.
 */
public abstract class S3Object extends S3Metadata{

    /**
     * Instantiate an S3 object with the given key.
     * @param key S3 object key.
     */
    public S3Object (String key)
    {
        this(key, DEFAULT_MEDIA_TYPE);
    }

    /**
     * Instantiate an S3 Object with the given key and media type.
     * @param key S3 object key.
     * @param mediaType Object's media type.
     */
    public S3Object (String key, MediaType mediaType)
    {
        this(key, mediaType, new HashMap<String,String>());
    }

    /**
     * Instantiate an S3 Object with the given key, media type, and metadata.
     * @param key S3 object key.
     * @param mediaType Object's media type.
     * @param metadata Object's metadata. Metadata keys must be a single, ASCII
     *     string, and may not contain spaces. Metadata values must also be ASCII,
     *     and any leading or trailing spaces may be stripped.
     */
    public S3Object (String key, MediaType mediaType, Map<String,String> metadata)
    {
        super(key, mediaType, metadata);
    }

    /**
     * Get the object's input stream, used to read object contents, potentially
     * from the remote S3 server. The caller is responsible for closing the
     * stream.
     */
    public abstract InputStream getInputStream ();
}
