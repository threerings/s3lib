//  This software code is made available "AS IS" without warranties of any
//  kind.  You may copy, display, modify and redistribute the software
//  code either by itself or as incorporated into your code; provided that
//  you do not remove any proprietary notices.  Your use of this software
//  code is at your own risk and you waive any claim against Amazon
//  Digital Services, Inc. or its affiliates with respect to your use of
//  this software code.
//
// (c) 2006 Three Rings Design, Inc.
// (c) 2006 Amazon Digital Services, Inc. or its affiliates.

package com.threerings.s3;

import org.apache.commons.codec.EncoderException;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpException;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;

import org.apache.commons.httpclient.protocol.Protocol;

import java.io.InputStream;
import java.io.IOException;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * An interface into the S3 system.  It is initially configured with
 * authentication and connection parameters and exposes methods to access and
 * manipulate S3 data.
 * TODO: URL encoding is totally missing.
 */
public class AWSAuthConnection
{

    /**
     * Create a new interface to interact with S3 with the given credentials.
     *
     * @param awsKeyId Your unique AWS user id.
     * @param awsSecretKey The secret string used to generate signatures
     *        for authentication.
     */
    public AWSAuthConnection (String awsKeyId, String awsSecretKey) {
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
    public AWSAuthConnection (String awsKeyId, String awsSecretKey, Protocol protocol) {
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
    public AWSAuthConnection (String awsKeyId, String awsSecretKey, Protocol protocol,
                             String host)
    {
        this(awsKeyId, awsSecretKey, protocol, S3Utils.DEFAULT_HOST, protocol.getDefaultPort());
    }

    /**
     * Create a new interface to interact with S3 with the given credentials and
     * connection parameters.
     *
     * @param awsKeyId Your unique AWS user id.
     * @param awsSecretKey The secret string used to generate signatures
     *        for authentication.
     * @param useSSL True if HTTPS should be used to connect to S3.
     * @param host Which host to connect to. Usually, this will be s3.amazonaws.com
     * @param port Port to connect to.
     */
     public AWSAuthConnection (String awsKeyId, String awsSecretKey, Protocol protocol,
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
    public AWSAuthConnection (String awsKeyId, String awsSecretKey,
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
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     * @param metadata A Map of String to List of Strings representing the s3
     * metadata for this bucket (can be null).
     */
    public void createBucket (String bucketName, Map<String,List<String>> headers)
        throws IOException, S3Exception
    {
        PutMethod method = new PutMethod("/" + bucketName);
        S3Utils.signAWSRequest(_awsKeyId, _awsSecretKey, method, null);            
        try {
            _executeS3Method(method);
        } finally {
            method.releaseConnection();
        }
    }
    
    /**
     * Deletes a bucket.
     * @param bucketName The name of the bucket to delete.
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     */
    public void deleteBucket (String bucketName, Map<String,List<String>> headers)
        throws IOException, S3Exception
    {
        DeleteMethod method = new DeleteMethod("/" + bucketName);
        S3Utils.signAWSRequest(_awsKeyId, _awsSecretKey, method, null);
        try {
            _executeS3Method(method);
        } finally {
            method.releaseConnection();
        }
    }
    
    /**
     * Upload a file-backed S3 Object.
     * @param bucketName Destination bucket.
     * @param object S3 Object.
     */
    public void putObject (String bucketName, S3FileObject object)
        throws IOException, S3Exception
    {
        PutMethod method = new PutMethod("/" + bucketName + "/" +
            object.getKey());

        // Set the request entity. We must also set the content-type
        // explicitly so that the header can be properly included
        // in the AWS request signature.
        method.setRequestEntity(new InputStreamRequestEntity(
            object.getInputStream(), object.length(), object.getMimeType()));
        method.setRequestHeader("Content-Type", object.getMimeType());
        
        // XXX Every object is public!!! XXX
        method.setRequestHeader(S3Utils.AMAZON_HEADER_ACL, S3Utils.AMAZON_CANNED_ACL_PUBLIC_READ);
        
        S3Utils.signAWSRequest(_awsKeyId, _awsSecretKey, method, null);
        try {
            _executeS3Method(method);
        } finally {
            method.releaseConnection();
        }
    }
    
    /**
     * Delete a remote S3 Object.
     * @param bucketName Remote bucket.
     * @param object S3 Object.
     */
    public void deleteObject (String bucketName, S3FileObject object)
        throws IOException, S3Exception
    {
        DeleteMethod method = new DeleteMethod("/" + bucketName + "/" + object.getKey());
        S3Utils.signAWSRequest(_awsKeyId, _awsSecretKey, method, null);
        
        try {
            _executeS3Method(method);
        } finally {
            method.releaseConnection();
        }
    }
    
    /**
     * Execute the provided method, translating any error response into the
     * appropriate S3Exception.
     * @param method HTTP method to execute.
     */
    protected void _executeS3Method (HttpMethod method)
        throws IOException, S3Exception
    {
        int statusCode = _awsHttpClient.executeMethod(method);
        if (!(statusCode >= HttpStatus.SC_OK &&
            statusCode < HttpStatus.SC_MULTIPLE_CHOICES)) {
            // Request failed, throw exception
            InputStream stream;
            byte[] errorDoc = new byte[S3_MAX_ERROR_SIZE];

            stream = method.getResponseBodyAsStream();
            if (stream == null) {
                // We should always receive a response!
                throw new S3Exception("S3 failed to return an error " +
                    "response for HTTP status code: "+ statusCode);
            }

            stream.read(errorDoc, 0, errorDoc.length);
            throw S3Exception.exceptionForS3Error(new String(errorDoc).trim());
        }
    }
    
    /** AWS Access ID. */
    protected String _awsKeyId;
    
    /** AWS Access Key. */
    protected String _awsSecretKey;
    
    /** S3 HTTP client. */
    protected HttpClient _awsHttpClient;
    
    /** Maximum size of S3's error output. Should never be larger than 2k!!! */
    protected static final int S3_MAX_ERROR_SIZE = 2048;
}
