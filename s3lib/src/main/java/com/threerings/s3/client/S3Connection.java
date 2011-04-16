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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.net.URLCodec;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.EofSensorInputStream;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.xml.sax.SAXException;

import com.threerings.s3.client.S3ClientException.InvalidURIException;
import com.threerings.s3.client.acl.AccessControlList;

/**
 * An interface into the S3 system.  It is initially configured with
 * authentication and connection parameters and exposes methods to access and
 * manipulate S3 data.
 *
 * S3Connection instances are thread-safe.
 */
public class S3Connection {

    /** Default connection and read timeout for our http connections in milliseconds. */
    public static final int DEFAULT_TIMEOUT_MILLIS = 2 * 60 * 1000;

    /**
     * Create a new S3 client connection, with the given credentials and the default connection
     * host parameters and timeout.
     *
     * Connections will be SSL encrypted.
     *
     * @param keyId Your unique AWS user id.
     * @param secretKey The secret string used to generate signatures
     *        for authentication.
     */
    public S3Connection (String keyId, String secretKey) {
        this(keyId, secretKey, S3Utils.createDefaultHost());
    }

    /**
     * Create a new S3 client connection, with the given credentials and connection host
     * parameters, but with the default timeout.
     *
     * @param keyId The your user key into AWS
     * @param secretKey The secret string used to generate signatures for authentication.
     * @param hostConfig HttpClient HostConfig.
     */
    public S3Connection (String keyId, String secretKey, HttpHost hostConfig)
    {
        this(keyId, secretKey, hostConfig, DEFAULT_TIMEOUT_MILLIS);
    }

    /**
     * Create a new S3 client connection, with the given credentials and timeout, but with the
     * default connection host parameters.
     *
     * @param keyId The your user key into AWS
     * @param secretKey The secret string used to generate signatures for authentication.
     * @param timeoutMillis Connection and read timeout for http connections in milliseconds
     */
    public S3Connection (String keyId, String secretKey, int timeoutMillis)
    {
        this(keyId, secretKey, S3Utils.createDefaultHost(), timeoutMillis);
    }

    /**
     * Create a new S3 client connection, with the given credentials and connection host
     * parameters.
     *
     * @param keyId The your user key into AWS
     * @param secretKey The secret string used to generate signatures for authentication.
     * @param hostConfig HttpClient HostConfig.
     * @param timeoutMillis Connection and read timeout for http connections in milliseconds
     */
    public S3Connection (String keyId, String secretKey, HttpHost hostConfig,
            int timeoutMillis)
    {
        this.keyId = keyId;
        this.secretKey = secretKey;
        this.httpHost = hostConfig;

        /* Configure the multi-threaded connection manager. Default to MAX_INT (eg, unlimited)
         * connections, as S3 is intended to support such use */
        ThreadSafeClientConnManager connMgr = new ThreadSafeClientConnManager();
        connMgr.setMaxTotal(Integer.MAX_VALUE);
        connMgr.setDefaultMaxPerRoute(Integer.MAX_VALUE);

        /* httpclient defaults to no timeout, which is troublesome if we ever drop our network
         * connection.  Give it a generous timeout to keep things moving. */
        HttpParams params = new BasicHttpParams();
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, timeoutMillis);
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeoutMillis);

        this.httpClient = new DefaultHttpClient(connMgr, params);
    }

    /**
     * Creates a new bucket.
     * @param bucketName The name of the bucket to create.
     */
    public void createBucket (String bucketName)
        throws S3Exception
    {
        executeAndConsumeS3Method(new HttpPut(createURI(bucketName)));
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
     * List a bucket's contents, with a maximum number of
     * returned entries.
     *
     * @param marker Indicates where in the bucket to begin listing. The
     *  list will only include keys that occur lexiocgraphically after marker.
     *  Specify null for no marker.
     * @param maxKeys Maximum number of keys to return. The server may return
     *  fewer keys, but never more. Specify 0 for no limit.
     */
    public S3ObjectListing listObjects (String bucketName, String marker, int maxKeys)
        throws S3Exception
    {
        return listObjects(bucketName, null, marker, maxKeys, null);
    }

    /**
     * List a bucket's contents.
     * @param prefix Limits response to keys beginning with the provided prefix.
     *  Specify null for no prefix.
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
        List<NameValuePair> parameters = new ArrayList<NameValuePair>(4);

        if (prefix != null) {
            parameters.add(new BasicNameValuePair(LIST_PREFIX_PARAMETER, prefix));
        }
        if (marker != null) {
            parameters.add(new BasicNameValuePair(LIST_MARKER_PARAMETER, marker));
        }
        if (maxKeys != 0) {
            parameters.add(new BasicNameValuePair(LIST_MAXKEYS_PARAMETER, Integer.toString(maxKeys)));
        }
        if (delimiter != null) {
            parameters.add(new BasicNameValuePair(LIST_DELIMITER_PARAMETER, delimiter));
        }

        HttpGet method = new HttpGet(createURI(bucketName, null, parameters));

        HttpResponse resp  = executeS3Method(method);
        InputStream content;
        try {
            content = resp.getEntity().getContent();
        } catch (IOException e) {
            method.abort();
            throw new S3ClientException("Error getting bucket GET response", e);
        }
        try {
            return new S3ObjectListing(content);
        } catch (SAXException se) {
            method.abort();
            throw new S3ClientException("Error parsing bucket GET response", se);
        } catch (IOException e) {
            method.abort();
            throw new S3ClientException("Error reading bucket GET response", e);
        } finally {
            try {
                content.close();
            } catch (IOException e) {}
        }
    }


    /**
     * Deletes a bucket.
     * @param bucketName The name of the bucket to delete.
     */
    public void deleteBucket (String bucketName)
        throws S3Exception
    {
        executeAndConsumeS3Method(new HttpDelete(createURI(bucketName)));
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
        putObject(bucketName, object, accessPolicy, new HashMap<String,String>());
    }

    /**
     * Upload an S3 Object.
     * @param bucketName Destination bucket.
     * @param object S3 Object.
     * @param accessPolicy S3 Object's access policy.
	 * @param headers http headers to be served with the object.
     */
    public void putObject (String bucketName, S3Object object,
        AccessControlList.StandardPolicy accessPolicy, Map<String,String> headers)
        throws S3Exception
    {
        byte[] checksum;

        HttpPut method = new HttpPut(createURI(bucketName, object.getKey()));

        // Set the request entity, handling unknown content lengths
        final MediaType mediaType = object.getMediaType();
        long length = object.length();

        method.setEntity(new InputStreamEntity(object.getInputStream(), length));
        method.addHeader(CONTENT_TYPE_HEADER, mediaType.getMimeType());
        // Set the content encoding
        if (mediaType.getContentEncoding() != null) {
          method.addHeader(CONTENT_ENCODING_HEADER, mediaType.getContentEncoding());
        }

        method.addHeader(S3Utils.ACL_HEADER, accessPolicy.toString());// Set the access policy

        // add any headers that were supplied
        for (Map.Entry<String,String> header : headers.entrySet()) {
            method.addHeader(header.getKey(), header.getValue());
        }

        // Compute and set the content-md5 value (base64 of 128bit digest)
        // http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.15
        try {
            byte[] md5 = object.getMD5();
            if (md5 != null) {
                checksum = Base64.encodeBase64(md5);
                method.addHeader(CONTENT_MD5_HEADER, new String(checksum, "ascii"));
            }
        } catch (UnsupportedEncodingException uee) {
            // ASCII must always be supported.
            throw new RuntimeException("Missing ASCII encoding");
        }

        // Set any metadata fields
        for (Map.Entry<String,String> entry : object.getMetadata().entrySet()) {
            String header = S3_METADATA_PREFIX + entry.getKey();
            method.addHeader(header, entry.getValue());
        }

        executeAndConsumeS3Method(method);
    }

    /**
     * Retrieve an S3Object, using the provided HttpMethodBase.
     *
     * @param objectKey The object key request, used to instantiate the returned S3Object.
     * @param method The HTTP method to execute.
     * @param hasBody Set to true if a response body is expected (eg, for an HTTP GET request)
     */
    private S3Metadata getObject (String bucketName, String objectKey, boolean hasBody)
    	throws S3Exception
    {
        URI uri = createURI(bucketName, objectKey);
        HttpRequestBase method;
        if (hasBody) {
            method = new HttpGet(uri);
        } else {
            method = new HttpHead(uri);
        }

        HttpResponse resp = executeS3Method(method);
        try {
            return buildS3Object(objectKey, hasBody, resp);
        } catch (S3Exception e) {
            method.abort();// Cleanup if something went wrong in extracting the metadata
            throw e;
        }
    }

    /**
     * Creates an S3Object from <code>resp</code> if hasBody is true, or an S3EmptyObject otherwise.
     * If an error is encountered in building the object, an S3Exception is thrown, but the
     * connection is left open. It's the responsibility of a caller to clean up the connection.
     */
    private S3Metadata buildS3Object (String objectKey, boolean hasBody, HttpResponse resp)
        throws S3Exception
    {
        HttpEntity entity = resp.getEntity();

        // Mime type
        String mimeType = getResponseHeader(resp, CONTENT_TYPE_HEADER, true);
        String contentEncoding = getResponseHeader(resp, CONTENT_ENCODING_HEADER, false);
        MediaType mediaType;
        if (contentEncoding != null) {
            mediaType = new MediaType(mimeType, contentEncoding);
        } else {
            mediaType = new MediaType(mimeType);
        }

        // Last modified
        String dateString = getResponseHeader(resp, LAST_MODIFIED_HEADER, false);
        long lastModified = 0L;
        try {
            if (dateString != null) {
                lastModified = RFC822Format.parse(dateString).getTime();
            }
        } catch (ParseException e) {}

        // Data length
        String lengthHeader = getResponseHeader(resp, CONTENT_LENGTH_HEADER, true);
        long length = Long.parseLong(lengthHeader);

        // MD5 Checksum. S3 returns this as the standard 128bit hex string, enclosed in quotes.
        byte[] digest;
        try {
            String hex = getResponseHeader(resp, S3_MD5_HEADER, true);
            // Strip the surrounding quotes
            hex = hex.substring(1, hex.length() - 1);
            digest = new Hex().decode(hex.getBytes("utf8"));
        } catch (DecoderException de) {
            throw new S3Exception("S3 returned an invalid " + S3_MD5_HEADER + " header: " + de);
        } catch (UnsupportedEncodingException uee) {
            // UTF8 must always be supported.
            throw new RuntimeException("Missing UTF8 encoding");
        }

        // Retrieve metadata
        Map<String, String> metadata = new HashMap<String, String>();
        for (Header header : resp.getAllHeaders()) {
            if (header.getName().startsWith(S3_METADATA_PREFIX)) {
                // Strip the S3 prefix
                String key = header.getName().substring(S3_METADATA_PREFIX.length());
                metadata.put(key, header.getValue());
            }
        }

        if (hasBody) {
            // Get the response body as an "auto closing" stream -- it will return the HTTP
            // connection to the pool when the stream is closed or the end of the stream is reached
            InputStream response;
            try {
                InputStream s = entity.getContent();
                response = new EofSensorInputStream(s, null);
            } catch (IOException ioe) {
                throw new S3ClientException.NetworkException(
                    "Error receiving object GET response: " + ioe.getMessage(), ioe);
            }

            // The client now needs to close the response stream to release this connection
            // back to the pool
            return new S3StreamObject(objectKey, mediaType, length, digest, metadata, response,
                lastModified);
        } else {
            // We just fetched the head, so the response didn't have an entity and the connection
            // was already returned to the pool
            return new S3EmptyObject(objectKey, mediaType, length, digest, metadata, lastModified);
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
        return (S3Object)getObject(bucketName, objectKey, true);
    }

    /**
     * Retrieve an S3Object's metadata. The data stream is not retrieved (a HEAD request is
     * performed).
     *
     * @param bucketName Source bucket.
     * @param objectKey Object key.
     */
    public S3Metadata getObjectMetadata (String bucketName, String objectKey)
        throws S3Exception
    {
        return getObject(bucketName, objectKey, false);
    }

    /**
     * Delete a remote S3 Object.
     * @param bucketName Remote bucket.
     * @param objectKey S3 object key.
     */
    public void deleteObject (String bucketName, String objectKey)
        throws S3Exception
    {
        executeAndConsumeS3Method(new HttpDelete(createURI(bucketName, objectKey)));
    }
    /**
     * Copies the object at <code>srcObjectKey</code> in <code>bucket</code> to
     * <code>destObjectKey</code> in the same bucket preserving its metadata and setting its
     * access to private.
     */
    public void copyObject (String srcObjectKey, String destObjectKey, String bucket)
        throws S3Exception
    {
        copyObject(srcObjectKey, destObjectKey, bucket, bucket);
    }

    /**
     * Copies the object at <code>srcObjectKey</code> in <code>srcBucket</code> to
     * <code>destObjectKey</code> in <code>destBucket</code>. The metadata on the destination are
     * the same as on the source and its access is set to private.
     */
    public void copyObject (String srcObjectKey, String destObjectKey, String srcBucket,
        String destBucket)
        throws S3Exception
    {

        copyObject(srcObjectKey, destObjectKey, srcBucket, destBucket,
            AccessControlList.StandardPolicy.PRIVATE);
    }

    /**
     * Copies the object at <code>srcObjectKey</code> in <code>srcBucket</code> to
     * <code>destObjectKey</code> in <code>destBucket</code>. The metadata on the destination are
     * the same as on the source and its access is set to the given policy.
     */
    public void copyObject (String srcObjectKey, String destObjectKey, String srcBucket,
        String destBucket, AccessControlList.StandardPolicy accessPolicy)
        throws S3Exception
    {
        copyObject(srcObjectKey, destObjectKey, srcBucket, destBucket, accessPolicy,
            null);
    }

    /**
     * Copies the object at <code>srcObjectKey</code> in <code>srcBucket</code> to
     * <code>destObjectKey</code> in <code>destBucket</code>. The metadata on the destination are
     * taken from the given map and its access is set to the given policy.
     */
    public void copyObject (String srcObjectKey, String destObjectKey, String srcBucket,
        String destBucket, AccessControlList.StandardPolicy accessPolicy,
        Map<String, String> metadata)
        throws S3Exception
    {

        HttpPut method = new HttpPut(createURI(destBucket, destObjectKey));

        method.addHeader(S3_COPY_SOURCE_HEADER, encodePath(srcBucket, srcObjectKey));

        // Set the access policy
        method.addHeader(S3Utils.ACL_HEADER, accessPolicy.toString());
        if (metadata != null) {
            method.addHeader(S3_COPY_METADATA_HEADER, S3_COPY_METADATA_REPLACE_VALUE);
            // Set any metadata fields
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                String header = S3_METADATA_PREFIX + entry.getKey();
                method.addHeader(header, entry.getValue());
            }
        } else {
            method.addHeader(S3_COPY_METADATA_HEADER, S3_COPY_METADATA_COPY_VALUE);
        }

        executeAndConsumeS3Method(method);
    }

    /**
     * Execute the provided method, translating any error response into the appropriate
     * S3Exception, and then releases the method's resources.
     */
    private void executeAndConsumeS3Method (HttpRequestBase method)
        throws S3Exception
    {
        try {
            EntityUtils.consume(executeS3Method(method).getEntity());
        } catch (IOException e) {
            method.abort();
            throw new S3ClientException("Error closing S3 entity", e);
        }
    }

    /**
     * Execute the provided method, translating any error response into the appropriate
     * S3Exception. If there's an error response, the connection is closed. Otherwise it's the
     * responsibility of the caller to close the entity of the returned HttpResponse to free the
     * resources.
     * @param method HTTP method to execute.
     */
    private HttpResponse executeS3Method (HttpRequestBase method)
        throws S3Exception
    {
        HttpResponse resp;

        // Sign the request
        S3Utils.signAWSRequest(keyId, secretKey, method, null);

        // Execute the request
        try {
            resp = httpClient.execute(method);
        } catch (IOException ioe) {
            method.abort();
            throw new S3ClientException.NetworkException("Network error executing S3 method: " +
                ioe.getMessage(), ioe);
        }

        int statusCode = resp.getStatusLine().getStatusCode();
        if (!(statusCode >= HttpStatus.SC_OK && statusCode < HttpStatus.SC_MULTIPLE_CHOICES)) {
            try {
                handleErrorResponse(method, resp, statusCode);
            } finally {
                method.abort();
            }
        }
        return resp;
    }

    /**
     * Constructs and throws an appropriate S3Exception from the given respose and statusCode.
     * The connection is left open, so the caller should clean up after the exception is thrown.
     */
    private void handleErrorResponse (HttpRequestBase method, HttpResponse resp, int statusCode)
        throws S3Exception
    {
        if (method instanceof HttpHead) {
            // HEAD calls don't include a response body, so the best we can do is to throw an
            // exception indicating the status code.  Happily, S3 does a good job of mapping
            // their errors to HTTP status codes, so things like 404 mean either the bucket
            // or key didn't exist
            throw S3ServerException.exceptionForS3ErrorCode(statusCode,
                "S3 returned status code " + statusCode + " for a HEAD request for "
                    + method.getURI());
        }
        // Request failed, throw exception.
        byte[] responseData = new byte[S3_MAX_ERROR_SIZE];
        int errorLen;

        try {
            InputStream stream = resp.getEntity().getContent();
            if (stream == null) {
                // We should always receive a response!
                throw new S3Exception("S3 failed to return an error response for HTTP status code: " + statusCode);
            }

            try {
                errorLen = stream.read(responseData, 0, responseData.length);
            } finally {
                stream.close();
            }
        } catch (IOException ioe) {
            throw new S3ClientException.NetworkException("Network error receiving S3 error response", ioe);
        }

        if (errorLen == S3_MAX_ERROR_SIZE) {
            // We didn't consume the entirety of the response, so we need to close the connection
            throw new S3Exception("S3 returned an error response longer than is valid");
        }

        // Trim the byte array to the response's length to make it a valid XML document.
        byte[] errorDoc = new byte[errorLen];
        System.arraycopy(responseData, 0, errorDoc, 0, errorLen);
        throw S3ServerException.exceptionForS3Error(errorDoc);
    }

    /**
     * Pull the header value out of the HTTP method response.
     */
    private String getResponseHeader (HttpResponse resp, String name, boolean required)
        throws S3Exception
    {
        Header[] headers = resp.getHeaders(name);
        if (headers.length == 0) {
            if (required) {
                throw new S3Exception("S3 failed to return a " + name + " header");
            } else {
                return null;
            }
        } else if (headers.length > 1) {
            throw new S3Exception("S3 returned " + headers.length + " " + name + " headers instead of 1");
        }

        return headers[0].getValue();
    }

    private URI createURI (String bucketName, String objectKey)
        throws InvalidURIException
    {
        return createURI(bucketName, objectKey, null);
    }


    private URI createURI (String path)
        throws InvalidURIException
    {
        return createURI(path, null, null);
    }

    private URI createURI (String bucket, String objectKey, List<NameValuePair> queryParams)
        throws InvalidURIException
    {
        String encodedPath = encodePath(bucket, objectKey);
        String query = queryParams == null ? "" : URLEncodedUtils.format(queryParams, "UTF-8");
        try {
            return URIUtils.createURI(httpHost.getSchemeName(), httpHost.getHostName(),
                httpHost.getPort(), encodedPath, query, null);
        } catch (URISyntaxException e) {
            throw new S3ClientException.InvalidURIException("Invalid URI for path " + encodedPath
                + " with params " + query, e);
        }
    }

    private String encodePath (String bucket, String objectKey)
        throws InvalidURIException
    {
        String encodedPath;
        try {
            encodedPath = "/" + _urlEncoder.encode(bucket);
        } catch (EncoderException e) {
            throw new S3ClientException.InvalidURIException(
                "Encoding error for bucket " + bucket, e);
        }
        if (objectKey != null) {
            try {
                encodedPath += "/" + _urlEncoder.encode(objectKey);
            } catch (EncoderException e) {
                throw new S3ClientException.InvalidURIException("Encoding error for object key "
                    + objectKey, e);
            }
        }
        return encodedPath;
    }

    /** AWS Access ID. */
    private final String keyId;

    /** AWS Access Key. */
    private final String secretKey;

    /** S3 HTTP client. */
    private final HttpClient httpClient;

    /** S3 HTTP client. */
    private final HttpHost httpHost;

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

    /** Last-Modified date header. */
    private static final String LAST_MODIFIED_HEADER = "Last-Modified";

    /** Mime Type Header. */
    private static final String CONTENT_TYPE_HEADER = "Content-Type";

    /** Content-Length Header. */
    private static final String CONTENT_LENGTH_HEADER = "Content-Length";

    /** Content Encoding Header. */
    private static final String CONTENT_ENCODING_HEADER = "Content-Encoding";

    /** Header for the MD5 digest in S3 GET responses. Not to be confused
     * with the Content-MD5 header that we use in PUT requests. */
    private static final String S3_MD5_HEADER = "ETag";

    /** Header prefix for object metadata. */
    private static final String S3_METADATA_PREFIX = "x-amz-meta-";

    /** Header prefix for object metadata. */
    private static final String S3_COPY_SOURCE_HEADER = "x-amz-copy-source";

    /** Header prefix for object metadata. */
    private static final String S3_COPY_METADATA_HEADER = "x-amz-metadata-directive";

    /** Header prefix for object metadata. */
    private static final String S3_COPY_METADATA_REPLACE_VALUE = "REPLACE";

    /** Header prefix for object metadata. */
    private static final String S3_COPY_METADATA_COPY_VALUE = "COPY";
}