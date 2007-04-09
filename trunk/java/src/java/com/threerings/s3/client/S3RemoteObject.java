/* 
 * S3RemoteObject vi:ts=4:sw=4:expandtab:
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
    public byte[] getMD5 ()
        throws S3ClientException
    {
        return _md5digest;
    }

    @Override // From S3Object
    public long length () {
        return _length;
    }
    
    /** Data length in bytes. */
    private final long _length;
    
    /** MD5 digest. */
    private final byte[] _md5digest;

    /** HTTP response stream. This is "auto-closing" -- the connection will
     * be closed when the stream is closed. */
    private final InputStream _response;
}
