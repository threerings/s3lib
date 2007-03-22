//  This software code is made available "AS IS" without warranties of any
//  kind.  You may copy, display, modify and redistribute the software
//  code either by itself or as incorporated into your code; provided that
//  you do not remove any proprietary notices.  Your use of this software
//  code is at your own risk and you waive any claim against Amazon
//  Digital Services, Inc. or its affiliates with respect to your use of
//  this software code.

// (c) 2006 Three Rings Design, Inc.
// (c) 2006 Amazon Digital Services, Inc. or its affiliates.

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
    
    /**
     * Instantiate an S3 object with the given key.
     * @param key S3 object key.
     */
    public S3Object (String key)
    {
        this(key, "binary/octet-stream");
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
     * @param metadata: Object's metadata. Metadata keys must be a single, ASCII
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
     * from the remote S3 server.
     */
    public abstract InputStream getInputStream () throws S3ClientException;

    /**
     * Get the object's MD5 checksum.
     */
    public abstract byte[] getMD5Checksum () throws S3ClientException;

    /**
     * Returns the number of bytes required to store the
     * S3 Object.
     */
    public abstract long length ();
    
    /** S3 object name. */
    protected String _key;
    
    /** S3 object mime-type. */
    protected String _mimeType;
    
    /** S3 object meta-data. */
    protected Map<String,String> _metadata;
}
