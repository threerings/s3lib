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
    {
        super(key);
        _file = file;
    }
    
    /**
     * Instantiate an S3 file-backed object with the given key.
     * @param key S3 object key.
     * @param file File backing.
     * @param mimeType Object's MIME type.
     */
    public S3FileObject(String key, File file, String mimeType)
    {
        super(key, mimeType);
        _file = file;
    }
    
    /**
     * Get input stream for the file, used to read file contents.
     */
    public FileInputStream getInputStream ()
        throws FileNotFoundException
    {
        return new FileInputStream(_file);
    }
    
    /**
     * Get output stream for the file, used to write object contents.
     */
    public FileOutputStream getOutputStream ()
        throws FileNotFoundException
    {
        return new FileOutputStream(_file);
    }
    
    // Documentation inherited
    public long length () {
        return _file.length();
    }
    
    /** File path. */
    protected File _file;
}
