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

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpException;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import org.apache.commons.httpclient.protocol.Protocol;

import java.io.InputStream;
import java.io.IOException;

import java.util.List;
import java.util.Map;

/**
 * An interface into the S3 system.  It is initially configured with
 * authentication and connection parameters and exposes methods to access and
 * manipulate S3 data.
 */
public class AWSAuthConnection
{

    /**
     * Create a new interface to interact with S3 with the given credentials.
     *
     * @param awsAccessKeyId The your user key into AWS
     * @param awsSecretAccessKey The secret string used to generate signatures
     *        for authentication.
     */
    public AWSAuthConnection (String awsAccessKeyId, String awsSecretAccessKey) {
        this(awsAccessKeyId, awsSecretAccessKey, Protocol.getProtocol("https"));
    }

    /**
     * Create a new interface to interact with S3 with the given credentials and
     * connection parameters.
     *
     * @param awsAccessKeyId The your user key into AWS
     * @param awsSecretAccessKey The secret string used to generate signatures
     *        for authentication.
     * @param protocol Protocol to use to connect to S3.
     */
    public AWSAuthConnection (String awsAccessKeyId, String awsSecretAccessKey, Protocol protocol) {
        this(awsAccessKeyId, awsSecretAccessKey, protocol, Utils.DEFAULT_HOST);
    }

    /**
     * Create a new interface to interact with S3 with the given credentials and
     * connection parameters.
     *
     * @param awsAccessKeyId The your user key into AWS
     * @param awsSecretAccessKey The secret string used to generate signatures
     *        for authentication.
     * @param protocol Protocol to use to connect to S3.
     * @param host Which host to connect to. Usually, this will be s3.amazonaws.com
     */
    public AWSAuthConnection (String awsAccessKeyId, String awsSecretAccessKey, Protocol protocol,
                             String host)
    {
        this(awsAccessKeyId, awsSecretAccessKey, protocol, Utils.DEFAULT_HOST, protocol.getDefaultPort());
    }

    /**
     * Create a new interface to interact with S3 with the given credentials and
     * connection parameters.
     *
     * @param awsAccessKeyId The your user key into AWS
     * @param awsSecretAccessKey The secret string used to generate signatures
     *        for authentication.
     * @param useSSL True if HTTPS should be used to connect to S3.
     * @param host Which host to connect to. Usually, this will be s3.amazonaws.com
     * @param port Port to connect to.
     */
     public AWSAuthConnection (String awsAccessKeyId, String awsSecretAccessKey, Protocol protocol,
                              String host, int port)
    {
        HostConfiguration awsHostConfig = new HostConfiguration();
        awsHostConfig.setHost(host, port, protocol);
        
        // Escape the tyranny of this() + implicit constructors.
        _init(awsAccessKeyId, awsSecretAccessKey, awsHostConfig);
    }

    /**
     * Create a new interface to interact with S3 with the given credential and connection
     * parameters
     *
     * @param awsAccessKeyId The your user key into AWS
     * @param awsSecretAccessKey The secret string used to generate signatures for authentication.
     * @param awsHostConfig HttpClient HostConfig.
     */
    public AWSAuthConnection (String awsAccessKeyId, String awsSecretAccessKey,
        HostConfiguration awsHostConfig)
    {
        // Escape the tyranny of this() + implicit constructors.
        _init(awsAccessKeyId, awsSecretAccessKey, awsHostConfig);
    }

    // Private initializer
    private void _init (String awsAccessKeyId, String awsSecretAccessKey,
        HostConfiguration awsHostConfig)
    {
        _awsAccessKeyId = awsAccessKeyId;
        _awsSecretAccessKey = awsSecretAccessKey;
        _awsHttpClient = new HttpClient();
        _awsHttpClient.setHostConfiguration(awsHostConfig);
    }
    
    /**
     * Creates a new bucket.
     * @param bucket The name of the bucket to create.
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     * @param metadata A Map of String to List of Strings representing the s3
     * metadata for this bucket (can be null).
     */
    public void createBucket (String bucket, Map<String,List<String>> headers)
        throws IOException
    {
        PutMethod method = new PutMethod("/" + bucket);
        try {
            int statusCode = _awsHttpClient.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
                // Throw correct exception
                _getExceptionForResponse(method);
            }
        } finally {
            method.releaseConnection();
        }
        // return new Response(makeRequest("PUT", bucket, headers));
    }
    
    /**
     * Deletes a bucket.
     * @param bucket The name of the bucket to delete.
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     */
    public void deleteBucket (String bucket, Map<String,List<String>> headers)
    {
        DeleteMethod method = new DeleteMethod("/" + bucket);
        //return new Response(makeRequest("DELETE", bucket, headers));
    }
    
    // Fish an exception out of a REST method response
    protected S3Exception _getExceptionForResponse (HttpMethod method)
        throws IOException
    {
        InputStream stream;
        byte[] output = new byte[S3_MAX_ERROR_SIZE];

        stream = method.getResponseBodyAsStream();
        if (stream == null) {
            // We should always receive a response
            return new S3Exception.S3InternalErrorException();
        }
        
        stream.read(output, 0, output.length);
        System.out.println(new String(output));
        
        return null;
    }
    
    /** AWS Access ID. */
    protected String _awsAccessKeyId;
    
    /** AWS Access Key. */
    protected String _awsSecretAccessKey;
    
    /** S3 HTTP client. */
    protected HttpClient _awsHttpClient;
    
    /** Maximum size of S3's error output. Should never be larger than 2k!!! */
    protected static final int S3_MAX_ERROR_SIZE = 2048;
}
