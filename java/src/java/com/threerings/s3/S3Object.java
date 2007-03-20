//  This software code is made available "AS IS" without warranties of any
//  kind.  You may copy, display, modify and redistribute the software
//  code either by itself or as incorporated into your code; provided that
//  you do not remove any proprietary notices.  Your use of this software
//  code is at your own risk and you waive any claim against Amazon
//  Digital Services, Inc. or its affiliates with respect to your use of
//  this software code.

// (c) 2006 Three Rings Design, Inc.
// (c) 2006 Amazon Digital Services, Inc. or its affiliates.

package com.threerings.s3;

import java.util.List;
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
        _key = key;
        _mimeType = mimeType;
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
     * Get the object's input stream, used to read object contents.
     */
    public abstract InputStream getInputStream () throws S3ClientException;
    
    /**
     * Get the object's output stream, used to write object contents.
     */
    public abstract OutputStream getOutputStream () throws S3ClientException;

    /**
     * Returns the number of bytes required to store the
     * S3 Object.
     */
    public abstract long length ();
    
    /** S3 object name. */
    protected String _key;
    
    /** S3 object mime-type. */
    protected String _mimeType;
    
    /** TODO: S3 object meta-data. */
    protected Map<String,List<String>> _metadata;
}
