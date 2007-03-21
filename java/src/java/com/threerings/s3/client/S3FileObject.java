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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

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
    }

    @Override // From S3Object
    public InputStream getInputStream ()
        throws S3ClientException
    {
        try {
            return new FileInputStream(_file);
        } catch (FileNotFoundException fnf) {
            throw new S3ClientException("File was not found.", fnf);
        }
    }
    

    @Override // From S3Object
    public OutputStream getOutputStream ()
        throws S3ClientException
    {
        try {
            return new FileOutputStream(_file);
        } catch (FileNotFoundException fnf) {
            throw new S3ClientException("File was not found.", fnf);
        }
    }

    @Override // From S3Object
    public byte[] getMD5Checksum ()
        throws S3ClientException
    {
         InputStream input;
         MessageDigest md;
         byte data[];
         int nbytes;
         byte digest[];

         // Initialize
         try {
             md = MessageDigest.getInstance("md5");             
         } catch (NoSuchAlgorithmException nsa) {
             // If MD5 isn't available, we're in trouble.
             throw new RuntimeException(nsa);
         }

         input = getInputStream();
         data = new byte[1024];

         // Compute the digest
         try {
             while ((nbytes = input.read(data)) > 0) {
                 md.update(data, 0, nbytes);
             }             
         } catch (IOException ioe) {
             throw new S3ClientException("Failure reading input file: " + ioe, ioe);
         }
         return md.digest();
    }

    @Override // From S3Object
    public long length () {
        return _file.length();
    }

    /** File path. */
    protected File _file;
}
