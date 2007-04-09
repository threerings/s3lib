/* 
 * S3FileObject vi:ts=4:sw=4:expandtab:
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
    public byte[] getMD5 ()
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
    private File _file;
}
