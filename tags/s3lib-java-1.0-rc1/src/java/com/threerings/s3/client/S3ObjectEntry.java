/* 
 * S3ObjectEntry vi:ts=4:sw=4:expandtab:
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

import java.util.Date;

/**
 * A simple represention of a single remote object stored in S3.
 */
public class S3ObjectEntry {

    /**
     * Create a new S3ObjectEntry instance, with the provided remote properties.
     */
    public S3ObjectEntry (String key, Date lastModified, String eTag, long size,
        String storageClass, S3Owner owner)
    {
        _key = key;
        _lastModified = lastModified;
        _size = size;
        _storageClass = storageClass;
        _owner = owner;
        _eTag = eTag;
    }

    /**
     * Returns the object's S3 key.
     */
    public String getKey () {
        return _key;
    }

    /**
     * Returns the date at which the object was last modified.
     */
    public Date getLastModified () {
        return _lastModified;
    }

    /**
     * Returns the object's ETag, which can be used for conditional GETs.
     */
    public String getETag () {
        return _eTag;
    }

    /**
     * Returns the size of the object in bytes.
     */
    public long getSize () {
        return _size;
    }

    /**
     * Returns the object's S3 storage class.
     */
    public String getStorageClass () {
        return _storageClass;
    }

    /**
     * Returns the object's S3 Owner.
     */
    public S3Owner getOwner () {
        return _owner;
    }

    @Override
    public String toString() {
        return _key;
    }

    /** The name of the object */
    private final String _key;

    /** The date at which the object was last modified. */
    private final Date _lastModified;

    /** The object's ETag, which can be used for conditional GETs. */
    private final String _eTag;

    /** The size of the object in bytes. */
    private final long _size;

    /** The object's storage class */
    private final String _storageClass;

    /** The object's owner */
    private final S3Owner _owner;
}
