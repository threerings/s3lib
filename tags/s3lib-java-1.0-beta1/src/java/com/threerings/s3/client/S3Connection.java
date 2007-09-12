/* 
 * S3Connection vi:ts=4:sw=4:expandtab:
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

import com.threerings.s3.client.acl.AccessControlList;

import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;

import org.apache.commons.httpclient.protocol.Protocol;

import org.xml.sax.SAXException;

/**
 * An interface into the S3 system.  It is initially configured with
 * authentication and connection parameters and exposes methods to access and
 * manipulate S3 data.
 *
 * @todo Implement a re-entrant HttpConnectionManager so that we can support
 *  multiple "in-flight" S3 objects on the same thread. Until this is written,
 *  a new S3Connection request will invalidate the previous request. This is
 *  only particularly important when using getObject(), as the request remains
 *  open to provide access to the data stream.
 */
public class S3Connection
{
    /**
     * Create a new interface to interact with S3 with the given credentials.
     *
     * @param awsKeyId Your unique AWS user id.
     * @param awsSecretKey The secret string used to generate signatures
     *        for authentication.
     */
    public S3Connection (String awsKeyId, String awsSecretKey) {
        this(awsKeyId, awsSecretKey, Protocol.getProtocol("https"));
    }

    /**
     * Create a new interface to interact with S3 with the given credentials and
     * connection parameters.
     *
     * @param awsKeyId Your unique AWS user id.
     * @param awsSecretKey The secret string used to generate signatures
     *        for authentication.
     * @param protocol Protocol to use to connect to S3.
     */
    public S3Connection (String awsKeyId, String awsSecretKey, Protocol protocol) {
        this(awsKeyId, awsSecretKey, protocol, S3Utils.DEFAULT_HOST);
    }

    /**
     * Create a new interface to interact with S3 with the given credentials and
     * connection parameters.
     *
     * @param awsKeyId Your unique AWS user id.
     * @param awsSecretKey The secret string used to generate signatures
     *        for authentication.
     * @param protocol Protocol to use to connect to S3.
     * @param host Which host to connect to. Usually, this will be s3.amazonaws.com
     */
    public S3Connection (String awsKeyId, String awsSecretKey, Protocol protocol,
                             String host)
    {
        this(awsKeyId, awsSecretKey, protocol, host, protocol.getDefaultPort());
    }

    /**
     * Create a new interface to interact with S3 with the given credentials and
     * connection parameters.
     *
     * @param awsKeyId Your unique AWS user id.
     * @param awsSecretKey The secret string used to generate signatures
     *        for authentication.
     * @param host Which host to connect to. Usually, this will be s3.amazonaws.com
     * @param port Port to connect to.
     */
     public S3Connection (String awsKeyId, String awsSecretKey, Protocol protocol,
                              String host, int port)
    {
        HostConfiguration awsHostConfig = new HostConfiguration();
        awsHostConfig.setHost(host, port, protocol);
        
        // Escape the tyranny of this() + implicit constructors.
        _init(awsKeyId, awsSecretKey, awsHostConfig);
    }

    /**
     * Create a new interface to interact with S3 with the given credential and connection
     * parameters
     *
     * @param awsKeyId The your user key into AWS
     * @param awsSecretKey The secret string used to generate signatures for authentication.
     * @param awsHostConfig HttpClient HostConfig.
     */
    public S3Connection (String awsKeyId, String awsSecretKey,
        HostConfiguration awsHostConfig)
    {
        // Escape the tyranny of this() + implicit constructors.
        _init(awsKeyId, awsSecretKey, awsHostConfig);
    }

    // Private initializer
    private void _init (String awsKeyId, String awsSecretKey,
        HostConfiguration awsHostConfig)
    {
        _awsKeyId = awsKeyId;
        _awsSecretKey = awsSecretKey;
        _awsHttpClient = new HttpClient();
        _awsHttpClient.setHostConfiguration(awsHostConfig);
    }
    
    /**
     * Creates a new bucket.
     * @param bucketName The name of the bucket to create.
     */
    public void createBucket (String bucketName)
        throws S3Exception
    {
        PutMethod method;
        try {
            method = new PutMethod("/" + _urlEncoder.encode(bucketName));
        } catch (EncoderException e) {
            throw new S3ClientException.InvalidURIException(
                "Encoding error for bucket " + bucketName + ": " + e);
        }

        try {
            executeS3Method(method);            
        } finally {
            method.releaseConnection();
        }
    }


    /**
     * List a bucket's contents. May return a truncated list.
     */
    public S3ObjectListing listObjects (String bucketName)
        throws S3Exception
    {
        return listObjects(bucketName, null, null, 0, null);
    }

    /**
     * List a bucket's contents.
     * @param prefix Limits response to keys beginning with the provided prefix.
     * @param marker Indicates where in the bucket to begin listing. The list
     *  will only include keys that occur lexicographically after marker.
     *  Specify null for no marker.
     * @param maxKeys Maximum number of keys to return. The server may return
     *  fewer keys, but never more. Specify 0 for no limit.
     * @param delimiter Keys that contain the same string between the prefix
     *  and the first occurence of the delimiter will be rolled up into a
     *  single result element in the CommonPrefixes data. Specify null for no
     *  delimiter.
     */
    public S3ObjectListing listObjects (String bucketName, String prefix, String marker, int maxKeys, String delimiter)
        throws S3Exception
    {
        GetMethod method;
        List<NameValuePair> parameters = new ArrayList<NameValuePair>(4);

        try {
            method = new GetMethod("/" + _urlEncoder.encode(bucketName));
        } catch (EncoderException e) {
            throw new S3ClientException.InvalidURIException(
                "Encoding error for bucket " + bucketName + ": " + e);            
        }
    
        if (prefix != null) {
            parameters.add(new NameValuePair(LIST_PREFIX_PARAMETER, prefix));
        }

        if (marker != null) {
            parameters.add(new NameValuePair(LIST_MARKER_PARAMETER, marker));
        }

        if (maxKeys != 0) {
            parameters.add(new NameValuePair(LIST_MAXKEYS_PARAMETER, Integer.toString(maxKeys)));
        }

        if (delimiter != null) {
            parameters.add(new NameValuePair(LIST_DELIMITER_PARAMETER, delimiter));
        }

        if (parameters.size() > 0) {
            method.setQueryString(
                (NameValuePair[]) parameters.toArray(new NameValuePair[parameters.size()])
            );
        }

        try {
            executeS3Method(method);
            return new S3ObjectListing(method.getResponseBodyAsStream());          
        } catch (SAXException se) {
            throw new S3ClientException("Error parsing bucket GET response: " + se.getMessage(), se);
        } catch (IOException ioe) {
            throw new S3ClientException.NetworkException("Error receiving bucket GET response: " +
                ioe.getMessage(), ioe);
        } finally {
            method.releaseConnection();
        }
    }


    /**
     * Deletes a bucket.
     * @param bucketName The name of the bucket to delete.
     */
    public void deleteBucket (String bucketName)
        throws S3Exception
    {
        DeleteMethod method;
        try {
            method = new DeleteMethod("/" +
                _urlEncoder.encode(bucketName));
        } catch (EncoderException e) {
            throw new S3ClientException.InvalidURIException(
                "Encoding error for bucket " + bucketName + ": " + e);
        }

        try {
            executeS3Method(method);            
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * Upload an S3 Object, using a PRIVATE access policy.
     * Equivalent to calling putObject(bucketName, object, AccessControlList.StandardPolicy.PRIVATE)
     *
     * @param bucketName Destination bucket.
     * @param object S3 Object.
     */
    public void putObject (String bucketName, S3Object object)
        throws S3Exception
    {
        putObject(bucketName, object, AccessControlList.StandardPolicy.PRIVATE);
    }

    /**
     * Upload an S3 Object.
     * @param bucketName Destination bucket.
     * @param object S3 Object.
     * @param accessPolicy S3 Object's access policy. 
     */
    public void putObject (String bucketName, S3Object object,
        AccessControlList.StandardPolicy accessPolicy)
        throws S3Exception
    {
        PutMethod method;
        byte[] checksum;

        try {
            method = new PutMethod("/" + _urlEncoder.encode(bucketName) +
                "/" + _urlEncoder.encode(object.getKey()));
        } catch (EncoderException e) {
            throw new S3ClientException.InvalidURIException(
                "Encoding error for bucket " + bucketName + " and key " +
                object.getKey() + ": " + e);
        }

        // Set the request entity
        method.setRequestEntity(new InputStreamRequestEntity(
            object.getInputStream(), object.length(), object.getMimeType()));

        // Set the access policy
        method.setRequestHeader(S3Utils.ACL_HEADER, accessPolicy.toString());

        // Compute and set the content-md5 value (base64 of 128bit digest)
        // http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.15
        try {
            checksum = Base64.encodeBase64(object.getMD5());
            method.setRequestHeader(CONTENT_MD5_HEADER, new String(checksum, "ascii"));            
        } catch (UnsupportedEncodingException uee) {
            // ASCII must always be supported.
            throw new RuntimeException("Missing ASCII encoding");
        }

        // Set any metadata fields
        for (Map.Entry<String,String> entry : object.getMetadata().entrySet()) {
            String header = S3_METADATA_PREFIX + entry.getKey();
            method.setRequestHeader(header, entry.getValue());
        }

        try {
            executeS3Method(method);            
        } finally {
            method.releaseConnection();
        }
    }


    /**
     * Retrieve a S3Object. The object's data streams directly from the remote
     * server, and thus may be invalidated.
     *
     * @param bucketName Source bucket.
     * @param objectKey Object key.
     */
    public S3Object getObject (String bucketName, String objectKey)
        throws S3Exception
    {
        GetMethod method;
        InputStream response;
        HashMap<String,String> metadata;
        String mimeType;
        byte digest[];
        long length;

        try {
            method = new GetMethod("/" + _urlEncoder.encode(bucketName) +
                "/" + _urlEncoder.encode(objectKey));
        } catch (EncoderException e) {
            throw new S3ClientException.InvalidURIException(
                "Encoding error for bucket " + bucketName + " and key " +
                objectKey + ": " + e);
        }

        // Execute the get request and retrieve all metadata from the response
        executeS3Method(method);

        // Mime type
        mimeType = getResponseHeader(method, CONTENT_TYPE_HEADER, true);

        // Data length
        length = method.getResponseContentLength();
        if (length == -1) {
            throw new S3Exception("S3 failed to supply the Content-Length header");            
        }

        // MD5 Checksum. S3 returns this as the standard 128bit hex string, enclosed
        // in quotes.
        try {
            String hex;
            
            hex = getResponseHeader(method, S3_MD5_HEADER, true);
            // Strip the surrounding quotes
            hex = hex.substring(1, hex.length() - 1);
            digest = new Hex().decode(hex.getBytes("utf8"));
        } catch (DecoderException de) {
            throw new S3Exception("S3 returned an invalid " + S3_MD5_HEADER + " header: " +
                de);
        } catch (UnsupportedEncodingException uee) {
            // UTF8 must always be supported.
            throw new RuntimeException("Missing UTF8 encoding");
        }

        // Retrieve metadata
        metadata = new HashMap<String,String>();
        for (Header header : method.getResponseHeaders()) {
            String name;

            name = header.getName();
            if (name.startsWith(S3_METADATA_PREFIX)) {
                // Strip the S3 prefix
                String key = name.substring(S3_METADATA_PREFIX.length());
                metadata.put(key, header.getValue());
            }
        }

        // Get the response body. This is an "auto closing" stream --
        // it will close the HTTP connection when the stream is closed.
        try {
            response = method.getResponseBodyAsStream();            
        } catch (IOException ioe) {
            throw new S3ClientException.NetworkException("Error receiving object GET response: " +
                ioe.getMessage(), ioe);
        }

        if (response == null) {
            // We should always receive a response!
            throw new S3Exception("S3 failed to return any document body");
        }

        return new S3RemoteObject(objectKey, mimeType, length, digest, metadata, response);
    }

    
    /**
     * Delete a remote S3 Object.
     * @param bucketName Remote bucket.
     * @param objectKey S3 object key.
     */
    public void deleteObject (String bucketName, String objectKey)
        throws S3Exception
    {
        DeleteMethod method;
        try {
            method = new DeleteMethod("/" +
                _urlEncoder.encode(bucketName) + "/" +
                _urlEncoder.encode(objectKey));
        } catch (EncoderException e) {
            throw new S3ClientException.InvalidURIException(
            "Encoding error for bucket " + bucketName + " and key " +
            objectKey + ": " + e);
        }

        try {
            executeS3Method(method);            
        } finally {
            method.releaseConnection();
        }
    }
    
    /**
     * Execute the provided method, translating any error response into the
     * appropriate S3Exception.
     * @param method HTTP method to execute.
     */
    protected void executeS3Method (HttpMethod method)
        throws S3Exception
    {
        int statusCode;
        
        // Sign the request
        S3Utils.signAWSRequest(_awsKeyId, _awsSecretKey, method, null);
        
        // Execute the request
        try {
            statusCode = _awsHttpClient.executeMethod(method);            
        } catch (IOException ioe) {
            throw new S3ClientException.NetworkException("Network error executing S3 method: " +
                ioe.getMessage(), ioe);
        }

        if (!(statusCode >= HttpStatus.SC_OK &&
            statusCode < HttpStatus.SC_MULTIPLE_CHOICES)) {
            // Request failed, throw exception
            InputStream stream;
            byte[] errorDoc = new byte[S3_MAX_ERROR_SIZE];

            try {
                stream = method.getResponseBodyAsStream();
                if (stream == null) {
                    // We should always receive a response!
                    throw new S3Exception("S3 failed to return an error " +
                        "response for HTTP status code: "+ statusCode);
                }

                stream.read(errorDoc, 0, errorDoc.length);
            } catch (IOException ioe) {
                throw new S3ClientException.NetworkException("Network error receiving S3 error response: " + ioe.getMessage(), ioe);
            }

            throw S3ServerException.exceptionForS3Error(new String(errorDoc).trim());
        }
    }

    /**
     * Pull the header value out of the HTTP method response.
     */
    private String getResponseHeader (HttpMethod method, String name, boolean required)
        throws S3Exception
    {
        Header header;

        header = method.getResponseHeader(name);
        if (header == null) {
            if (required) {
                throw new S3Exception("S3 failed to return a " + name + " header");
            } else {
                return null;
            }
        }

        return header.getValue();
    }

    /** AWS Access ID. */
    private String _awsKeyId;
    
    /** AWS Access Key. */
    private String _awsSecretKey;
    
    /** S3 HTTP client. */
    private HttpClient _awsHttpClient;
    
    /** URL encoder. */
    private final URLCodec _urlEncoder = new URLCodec();

    /** Prefix parameter. */
    private static final String LIST_PREFIX_PARAMETER = "prefix";

    /** Marker parameter. */
    private static final String LIST_MARKER_PARAMETER = "marker";
    
    /** Max Keys parameter. */
    private static final String LIST_MAXKEYS_PARAMETER = "max-keys";

    /** Delimiter parameter. */
    private static final String LIST_DELIMITER_PARAMETER = "delimiter";

    /** Maximum size of S3's error output. Should never be larger than 2k!!! */
    private static final int S3_MAX_ERROR_SIZE = 2048;

    /** Header for MD5 checksum validation. */
    private static final String CONTENT_MD5_HEADER = "Content-MD5";

    /** Mime Type Header. */
    private static final String CONTENT_TYPE_HEADER = "Content-Type";

    /** Header for the MD5 digest in S3 GET responses. Not to be confused
     * with the Content-MD5 header that we use in PUT requests. */
    private static final String S3_MD5_HEADER = "ETag";

    /** Header prefix for object metadata. */
    private static final String S3_METADATA_PREFIX = "x-amz-meta-";
}