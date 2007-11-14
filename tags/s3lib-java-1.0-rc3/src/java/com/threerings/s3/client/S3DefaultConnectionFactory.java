/* 
 * S3SimpleConnectionFactory.java vi:ts=4:sw=4:expandtab:
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

import org.apache.commons.httpclient.HostConfiguration;

/**
 * A simple, re-usable implementation of a S3 connection factory.
 *
 */
public class S3DefaultConnectionFactory implements S3ConnectionFactory {
    /**
     * Create a new connection factory. Connections will
     * be instantiated with the given credentials, using the default
     * AWS S3 host parameters.
     * 
     * Connections will be SSL encrypted.
     * 
     * @param keyId Your unique AWS user id.
     * @param secretKey The secret string used to generate signatures
     *        for authentication.
     */
    public S3DefaultConnectionFactory (String keyId, String secretKey) {
        this(keyId, secretKey, S3Utils.createDefaultHostConfig());
    }

    /**
     * Create a new S3 client connection, with the given credentials and connection
     * host parameters.
     *
     * @param keyId The your user key into AWS
     * @param secretKey The secret string used to generate signatures for authentication.
     * @param hostConfig HttpClient HostConfig.
     */
    public S3DefaultConnectionFactory (String keyId, String secretKey, HostConfiguration hostConfig)
    {
        this.keyId = keyId;
        this.secretKey = secretKey;
        this.hostConfig = hostConfig;
    }

    /**
     * 
     */
    /* (non-Javadoc)
     * @see com.threerings.s3.client.S3ConnectionFactory#createConnection()
     */
    public S3Connection createConnection() {
        return new S3Connection(keyId, secretKey, hostConfig);
    }

    /** AWS Access ID. */
    private final String keyId;
    
    /** AWS Access Key. */
    private final String secretKey;
    
    /** AWS S3 HTTP client host configuration. */
    private final HostConfiguration hostConfig;
}
