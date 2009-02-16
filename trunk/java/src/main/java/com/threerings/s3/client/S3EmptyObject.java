/* 
 * S3EmptyObject vi:ts=4:sw=4:expandtab:
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * A representation of an S3Object whose contents are unavailable
 * (eg, retrieved via a HEAD request). The returned InputStream
 * will always throw an IOException.
 */
class S3EmptyObject extends S3Object {
	/**
	 * A simple stream that always returns an IOException.
	 */
	private static class NullInputStream extends InputStream {
		@Override
		public int read() throws IOException {
			throw new IOException("No stream available");
		}
	}
    
    /**
     * Instantiate an S3 object with the given key.
     */
    public S3EmptyObject (String key, String mimeType, long length, byte[] digest,
        Map<String,String> metadata, long lastModified)
    {
        super(key, mimeType, metadata);
        _lastModified = lastModified;
        _length = length;
        _md5digest = digest;
    }
	
	@Override
	public InputStream getInputStream() throws S3ClientException {
		return new NullInputStream();
	}
	
	@Override
	public long lastModified () {
	    return _lastModified;
	}
	
	@Override
	public byte[] getMD5() throws S3ClientException {
		return _md5digest;
	}
	
	@Override
	public long length() {
		return _length;
	}
	
	/** Last modified timestamp. */
	private final long _lastModified;

    /** Data length in bytes. */
    private final long _length;
    
    /** MD5 digest. */
    private final byte[] _md5digest;
}
