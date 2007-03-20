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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;

import java.util.List;
import java.util.Map;

/**
 * A representation of a (locally file-backed) object stored in S3.
 */
public class S3FileObject extends S3Object {
    
    /**
     * Instantiate an S3 file-backed object with the given key.
     * @param key S3 object key.
     * @param file File backing.
     */
    public S3FileObject(String key, File file)
        throws FileNotFoundException
    {
        super(key);
        _file = file;
        _input = new FileInputStream(file);
        _output = new FileOutputStream(file);
    }
    
    /**
     * Instantiate an S3 file-backed object with the given key.
     * @param key S3 object key.
     * @param file File backing.
     * @param mimeType Object's MIME type.
     */
    public S3FileObject(String key, File file, String mimeType)
        throws FileNotFoundException
    {
        super(key, mimeType);
        _file = file;
        _input = new FileInputStream(file);
        _output = new FileOutputStream(file);
    }
    
    @Override // From S3Object
    public FileInputStream getInputStream ()
    {
        return _input;
    }
    

    @Override // From S3Object
    public FileOutputStream getOutputStream ()
    {
        return _output;
    }
    
    // Documentation inherited
    public long length () {
        return _file.length();
    }

    /** File path. */
    protected File _file;

    /** Input stream. */
    protected FileInputStream _input;

    /** Output stream. */
    protected FileOutputStream _output;
}
