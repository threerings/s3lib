//  This software code is made available "AS IS" without warranties of any
//  kind.  You may copy, display, modify and redistribute the software
//  code either by itself or as incorporated into your code; provided that
//  you do not remove any proprietary notices.  Your use of this software
//  code is at your own risk and you waive any claim against Amazon
//  Digital Services, Inc. or its affiliates with respect to your use of
//  this software code.

// (c) 2007 Three Rings Design, Inc.

package com.threerings.s3.client;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.List;
import java.util.Map;

/**
 * A representation of a remotely backed S3 object.
 * The input stream streams directly from the remote server.
 */
class S3RemoteObject extends S3Object {
    /**
     * Instantiate an S3 remote object with the given key.
     */
     
    public S3RemoteObject(String key, String mimeType, long length,
        byte[] digest, Map<String,String> metadata, InputStream response)
    {
        super(key, mimeType, metadata);
        _length = length;
        _md5digest = digest;
        _response = response;
    }
     

    @Override // From S3Object
    public InputStream getInputStream ()
        throws S3ClientException
    {
        return _response;
    }

    @Override // From S3Object
    public byte[] getMD5Checksum ()
        throws S3ClientException
    {
        return _md5digest;
    }

    @Override // From S3Object
    public long length () {
        return _length;
    }
    
    /** Data length in bytes. */
    protected long _length;
    
    /** MD5 digest. */
    protected byte[] _md5digest;

    /** HTTP response stream. This is "auto-closing" -- the connection will
     * be closed when the stream is closed. */
    protected InputStream _response;
}
