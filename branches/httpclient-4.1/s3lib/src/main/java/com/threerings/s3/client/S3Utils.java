/*
 * S3ClientException vi:ts=4:sw=4:expandtab:
 *
 * Copyright (c) 2005 - 2007 Three Rings Design, Inc.
 * Copyright (c) 2006 Amazon Digital Services, Inc. or its affiliates.
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

import org.apache.commons.codec.binary.Base64;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpRequestBase;

import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Mac;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

class S3Utils {
    /** Header prefix for generic S3 headers. */
    static final String AMAZON_HEADER_PREFIX = "x-amz-";

    /** Header for S3's alternate date. */
    static final String ALTERNATIVE_DATE_HEADER = "x-amz-date";

    /** Header for S3 access settings. */
    static final String ACL_HEADER = "x-amz-acl";

    /** Default AWS S3 Host. */
    static final String DEFAULT_HOST = "s3.amazonaws.com";

    /**
     * Helper method to create and initialize a {@link HostConfiguration} instance.
     * Always uses SSL.
     */
    static HttpHost createDefaultHost () {
        // Use -1 to specify the scheme default port
        return new HttpHost(DEFAULT_HOST, -1, "https");
    }

    /**
     * Sign (SHA-1 HMAC) a given AWS web request using the provided key.
     * The canonical request format used for signing is defined by the
     * Amazon S3 documentation:
     *  http://docs.amazonwebservices.com/AmazonS3/2006-03-01/RESTAuthentication.html
     *
     * TODO: Fix expires vs. date handling.
     *
     * @param awsKeyId AWS ID
     * @param awsSecretKey The secret string used to generate the HMAC.
     * @param method The HTTP method (request) to sign.
     * @param expires The expiration date for the signature
     */
    public static void signAWSRequest (String awsKeyId, String awsSecretKey,
        HttpRequestBase method, Date expires)
    {

        StringBuilder buf = new StringBuilder();

        // Set the required Date header (now)
        method.addHeader("Date", RFC822Format.format(new Date()));

        // Append method "verb"
        buf.append(method.getMethod() + "\n");

        // Add all interesting headers to a list, then sort them.  "Interesting"
        // is defined as Content-MD5, Content-Type, Date, and x-amz-
        SortedMap<String,String> interestingHeaders = new TreeMap<String,String>();
        for (Header header : method.getAllHeaders()) {
            String key = header.getName().toLowerCase();

            // Pull out only the headers that should be included in the signature.
            if (key.equals("content-type") || key.equals("content-md5") ||
                key.equals("date") ||
                key.startsWith(AMAZON_HEADER_PREFIX)) {

                // Stow the header
                interestingHeaders.put(key, header.getValue().trim());
            }
        }

        // If an AWS date header was specified, it should be used for the Date
        // header.
        if (interestingHeaders.containsKey(ALTERNATIVE_DATE_HEADER)) {
            interestingHeaders.put("date", "");
        }

        // If the expires is non-null, use that for the date field. This
        // trumps the x-amz-date behavior.
        if (expires != null) {
            String expires822 = RFC822Format.format(expires);
            interestingHeaders.put("date", expires822);
            // Set the expires header
            method.addHeader("Expires", expires822);
        }

        // these headers require that we still put a new line in after them,
        // even if they don't exist.
        if (!interestingHeaders.containsKey("content-type")) {
            interestingHeaders.put("content-type", "");
        }

        if (!interestingHeaders.containsKey("content-md5")) {
            interestingHeaders.put("content-md5", "");
        }

        // Finally, add all the interesting headers (i.e.: all that startwith x-amz- ;-))
        for (String key : interestingHeaders.keySet()) {
            if (key.startsWith(AMAZON_HEADER_PREFIX)) {
                buf.append(key).append(':').append(interestingHeaders.get(key));
            } else {
                buf.append(interestingHeaders.get(key));
            }
            buf.append("\n");
        }

        // Don't include the query parameters...
        String path = method.getURI().getRawPath();
        int queryIndex = path.indexOf('?');
        if (queryIndex == -1) {
            buf.append(path);
        } else {
            buf.append(path.substring(0, queryIndex));
        }

        // ...unless there is an acl or torrent parameter
        if (path.matches(".*[&?]acl($|=|&).*")) {
            buf.append("?acl");
        } else if (path.matches(".*[&?]torrent($|=|&).*")) {
            buf.append("?torrent");
        }

        // Finally, sign and encode the canonicalized headers
        SecretKeySpec signingKey = new SecretKeySpec(awsSecretKey.getBytes(), HMAC_SHA1_ALGORITHM);

        // Initialize a MAC instance with the signing key
        Mac mac;
        try {
            mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            // Should never happen
            throw new RuntimeException("Could not find SHA-1 algorithm.");
        }

        try {
            mac.init(signingKey);
        } catch (InvalidKeyException e) {
            // Also should not happen
            throw new RuntimeException("Could not initialize the MAC algorithm.", e);
        }

        // Compute the HMAC
        byte[] raw;
        try {
            raw = buf.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Couldn't find UTF-8?", e);
        }
        String b64 = new String(Base64.encodeBase64(mac.doFinal(raw)));

        // Insert the header
        method.addHeader(S3Utils.AUTH_HEADER, "AWS " + awsKeyId + ":" + b64);
    }

    /** AWS Authorization Header Name. */
    protected static final String AUTH_HEADER = "Authorization";

    /** HMAC/SHA1 Algorithm per RFC 2104. */
    protected static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
}
