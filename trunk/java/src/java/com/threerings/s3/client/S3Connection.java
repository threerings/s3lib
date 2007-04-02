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

package com.threerings.s3.client;

import com.threerings.s3.client.acl.AccessControlList;

import java.io.InputStream;
import java.io.IOException;

import java.util.Date;
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
import org.apache.commons.httpclient.HttpException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
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
 * TODO: URL encoding is totally missing.
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
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     * @param metadata A Map of String to List of Strings representing the s3
     * metadata for this bucket (can be null).
     */
    public void createBucket (String bucketName, Map<String,List<String>> headers)
        throws IOException, S3Exception
    {
        PutMethod method;
        try {
            method = new PutMethod("/" + _urlEncoder.encode(bucketName));
        } catch (EncoderException e) {
            throw new S3ClientException.InvalidURIException(
                "Encoding error for bucket " + bucketName + ": " + e);
        }

        executeS3Method(method);
    }


    /**
     * List a bucket's contents.
     */
    public S3ObjectListing listObjects (String bucketName)
        throws IOException, S3Exception
    {
        GetMethod method;
        InputStream stream;
        
        try {
            method = new GetMethod("/" + _urlEncoder.encode(bucketName));
        } catch (EncoderException e) {
            throw new S3ClientException.InvalidURIException(
                "Encoding error for bucket " + bucketName + ": " + e);            
        }

        executeS3Method(method);

        try {
            return new S3ObjectListing(method.getResponseBodyAsStream());          
        } catch (SAXException se) {
            throw new S3ClientException("Error parsing bucket GET response: " + se, se);
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
        DeleteMethod method;
        try {
            method = new DeleteMethod("/" +
                _urlEncoder.encode(bucketName));
        } catch (EncoderException e) {
            throw new S3ClientException.InvalidURIException(
                "Encoding error for bucket " + bucketName + ": " + e);
        }

        executeS3Method(method);
    }
    
    /**
     * Upload a file-backed S3 Object.
     * @param bucketName: Destination bucket.
     * @param object: S3 Object.
     * @param accessPolicy: S3 Object's access policy. 
     */
    public void putObject (String bucketName, S3Object object,
        AccessControlList.StandardPolicy accessPolicy)
        throws IOException, S3Exception
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
        method.setRequestHeader(AccessControlList.StandardPolicy.AMAZON_HEADER,
            accessPolicy.toString());

        // Compute and set the content-md5 value (base64 of 128bit digest)
        // http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.15
        checksum = Base64.encodeBase64(object.getMD5Checksum());
        method.setRequestHeader(CONTENT_MD5_HEADER, new String(checksum, "ascii"));

        // Set any metadata fields
        for (Map.Entry<String,String> entry : object.getMetadata().entrySet()) {
            String header = S3_METADATA_PREFIX + entry.getKey();
            method.setRequestHeader(header, entry.getValue());
        }

        executeS3Method(method);
    }


    /**
     * Retrieve a S3Object.
     * @param bucketName: Source bucket.
     * @param objectKey: Object key.
     */
    public S3RemoteObject getObject (String bucketName, String objectKey)
        throws IOException, S3Exception
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
        response = method.getResponseBodyAsStream();
        if (response == null) {
            // We should always receive a response!
            throw new S3Exception("S3 failed to return any document body");
        }

        return new S3RemoteObject(objectKey, mimeType, length, digest, metadata, response);
    }

    
    /**
     * Delete a remote S3 Object.
     * @param bucketName Remote bucket.
     * @param object S3 Object.
     */
    public void deleteObject (String bucketName, String key)
        throws IOException, S3Exception
    {
        DeleteMethod method;
        try {
            method = new DeleteMethod("/" +
                _urlEncoder.encode(bucketName) + "/" +
                _urlEncoder.encode(key));
        } catch (EncoderException e) {
            throw new S3ClientException.InvalidURIException(
            "Encoding error for bucket " + bucketName + " and key " +
            key + ": " + e);
        }

        executeS3Method(method);
    }
    
    /**
     * Execute the provided method, translating any error response into the
     * appropriate S3Exception.
     * @param method HTTP method to execute.
     */
    protected void executeS3Method (HttpMethod method)
        throws IOException, S3Exception
    {
        int statusCode;
        
        // Sign the request
        S3Utils.signAWSRequest(_awsKeyId, _awsSecretKey, method, null);
        
        // Execute the request
        statusCode = _awsHttpClient.executeMethod(method);
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
            method.releaseConnection();
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
    protected String _awsKeyId;
    
    /** AWS Access Key. */
    protected String _awsSecretKey;
    
    /** S3 HTTP client. */
    protected HttpClient _awsHttpClient;
    
    /** URL encoder. */
    protected URLCodec _urlEncoder = new URLCodec();
    
    /** Maximum size of S3's error output. Should never be larger than 2k!!! */
    protected static final int S3_MAX_ERROR_SIZE = 2048;

    /** Header for MD5 checksum validation. */
    protected static final String CONTENT_MD5_HEADER = "Content-MD5";

    /** Mime Type Header. */
    protected static final String CONTENT_TYPE_HEADER = "Content-Type";

    /** Header for the MD5 digest in S3 GET responses. Not to be confused
     * with the Content-MD5 header that we use in PUT requests. */
    protected static final String S3_MD5_HEADER = "ETag";

    /** Header prefix for object metadata. */
    static final String S3_METADATA_PREFIX = "x-amz-meta-";
}