//  This software code is made available "AS IS" without warranties of any
//  kind.  You may copy, display, modify and redistribute the software
//  code either by itself or as incorporated into your code; provided that
//  you do not remove any proprietary notices.  Your use of this software
//  code is at your own risk and you waive any claim against Amazon
//  Digital Services, Inc. or its affiliates with respect to your use of
//  this software code.

// (c) 2007 Three Rings Design, Inc.

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
    protected String _key;

    /** The date at which the object was last modified. */
    protected Date _lastModified;

    /** The object's ETag, which can be used for conditional GETs. */
    protected String _eTag;

    /** The size of the object in bytes. */
    protected long _size;

    /** The object's storage class */
    protected String _storageClass;

    /** The object's owner */
    protected S3Owner _owner;
}
