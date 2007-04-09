/* 
 * RemoteStream.java vi:ts=4:sw=4:expandtab:
 *
 * Copyright (c) 2007 Three Rings Design, Inc.
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

package com.threerings.s3.pipe;

import com.threerings.s3.client.acl.AccessControlList;
import com.threerings.s3.client.S3ByteArrayObject;
import com.threerings.s3.client.S3Connection;
import com.threerings.s3.client.S3Exception;
import com.threerings.s3.client.S3Object;
import com.threerings.s3.client.S3ObjectEntry;
import com.threerings.s3.client.S3ObjectListing;
import com.threerings.s3.client.S3ServerException;

import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.apache.commons.codec.binary.Base64;


/*
 * Remote stream management.
 *
 * S3 provides no functionality for locking or transactional behavior, thus, there is
 * no way of preventing a client from destroying a stream by attempting to modify it
 * concurrently. Individual streams are not "thread-safe".
 *
 * Stream key names are heirarchical, using "." as a delimiter, in order to leverage S3's
 * support for condensing common prefixes and dropping suffixes when listing objects.
 * This allows for the creation of per-stream "structures". Example:
 *  stream.info
 *  stream.block.0
 *  stream.block.1
 *
 * We base64-encode the stream name to ensure that it does not contain any "." delimiters.
 *
 * @todo: Investigate the atomicity of ACLs and their potential use for locking.
 */
public class RemoteStream {

    /**
     * Returns a list of all remote streams.
     */
    static public List<RemoteStreamInfo> getAllStreams (S3Connection connection,
        String bucketName)
        throws S3Exception, RemoteStreamException
    {
        List<RemoteStreamInfo> streams;
        S3ObjectListing listing;
        String marker = null;

        streams = new ArrayList<RemoteStreamInfo>();
        do {

            /* Get a listing of all common prefixes: STREAM_PREFIX.<encoded stream name>. */
            listing = connection.listObjects(bucketName, STREAM_PREFIX + FIELD_DELIMETER,
                marker, 1000, FIELD_DELIMETER);

            /* For each prefix, extract the stream info. */
            for (String prefix : listing.getCommonPrefixes()) {
                try {
                    RemoteStreamInfo info = getStreamInfo(connection, bucketName, prefix + INFO_FIELD);
                    streams.add(info);
                } catch (RemoteStreamException e) {
                    System.err.println("Skipping invalid remote stream " + prefix + INFO_FIELD +
                        ": " + e.getMessage());
                }
            }

            marker = listing.getNextMarker();
        } while (listing.truncated());

        return streams;
    }

    /**
     * Given the S3 stream info key, retrieve a remote stream info record.
     *
     * @throws RemoteStreamException.UnsupportedVersionException if the info
     * record version is unsupported.
     */
    public static RemoteStreamInfo getStreamInfo (S3Connection connection, String bucketName, String infoKey)
        throws S3Exception, RemoteStreamException
    {
        try {
            /* Fetch the stream's info record. */
            S3Object object;
            Map<String,String> metadata;

            /* Fetch the object. */
            object = connection.getObject(bucketName, infoKey);
            metadata = object.getMetadata();

            /* Extract the version and ensure we support it */
            String versionString;
            int version;
            if ((versionString = metadata.get(INFO_KEY_VERSION)) == null) {
                throw new RemoteStreamException.InvalidInfoRecordException(
                    "Stream missing version number.");
            }
            version = Integer.parseInt(versionString);
            if (version != VERSION) {
                throw new RemoteStreamException.UnsupportedVersionException(
                    "Stream record version is not supported: " + versionString + ".");
            }

            /* Extract the stream name. */
            String name;
            if ((name = metadata.get(INFO_KEY_NAME)) == null) {
                throw new RemoteStreamException.InvalidInfoRecordException(
                    "Stream missing stream name.");
            }

            /* Extract the creation date. */
            String createdString;
            Date created;
            if ((createdString = metadata.get(INFO_KEY_CTIME)) == null) {
                throw new RemoteStreamException.InvalidInfoRecordException(
                    "Stream missing stream creation date");
            }
            created = new Date(Long.parseLong(createdString));

            return new RemoteStreamInfo(name, version, created);

        } catch (S3ServerException.NoSuchKeyException nsk) {
            return null;
        } catch (NumberFormatException nfe) {
            /* Shouldn't happen, unless intentionally bad data is supplied. */
            throw new RemoteStreamException.InvalidInfoRecordException("Unsupported integer: " + nfe);
        }
    }

    public RemoteStream (S3Connection connection, String bucketName,
        String streamName)
    {
        _connection = connection;
        _bucketName = bucketName;
        _streamName = streamName;

        /* Encode the stream name, cache the result. */
        try {
            Base64 encoder = new Base64();
            byte[] data = _streamName.getBytes(NAME_ENCODING);
            _encodedStreamName = new String(encoder.encode(data), "ascii");            
        } catch (UnsupportedEncodingException uee) {
            // utf-8 and ascii must always be available.
            throw new RuntimeException("Missing a standard encoding", uee);
        }
    }


    /**
     * Initialize remote stream info record.
     *
     * We currently use Amazon's metadata support to define simple key-value pairs
     * that are attached to a zero-length object.
     */
    public void putStreamInfo ()
        throws S3Exception, RemoteStreamException
    {
        Map<String,String> metadata = new HashMap<String,String>();
        S3ByteArrayObject infoObject = new S3ByteArrayObject(streamInfoKey(), new byte[0], S3Object.DEFAULT_MIME_TYPE);

        /* Set the name. */
        metadata.put(INFO_KEY_NAME, _streamName);

        /* Set the version. */
        metadata.put(INFO_KEY_VERSION, Integer.toString(VERSION));

        /* Set the creation date. */
        metadata.put(INFO_KEY_CTIME, Long.toString(new Date().getTime()));

        /* Upload the info object. */
        infoObject.setMetadata(metadata);
        _connection.putObject(_bucketName, infoObject, AccessControlList.StandardPolicy.PRIVATE);
    }


    /**
     * Retrieve a remote stream info record.
     * Will return null if the stream is not found.
     *
     * Throws RemoteStreamException.UnsupportedVersionException if the info
     * record version is unsupported.
     */
    public RemoteStreamInfo getStreamInfo ()
        throws S3Exception, RemoteStreamException
    {
        return getStreamInfo(_connection, _bucketName, streamInfoKey());
    }

    /**
     * Delete the remote stream data.
     * May be called multiple times if a failure occurs.
     */
    public void delete ()
        throws S3Exception, RemoteStreamException
    {
        S3ObjectListing listing;
        String marker = null;

        do {
            /* List and delete all stream keys. */
            listing = _connection.listObjects(_bucketName, streamPrefix(), marker, 1000, null);

            for (S3ObjectEntry entry : listing.getEntries()) {
                _connection.deleteObject(_bucketName, entry.getKey());
            }

            marker = listing.getNextMarker();
        } while (listing.truncated());
    }


    /**
     * Return the complete S3 prefix for this stream.
     */
    public String streamPrefix () {
        return STREAM_PREFIX + FIELD_DELIMETER + _encodedStreamName + FIELD_DELIMETER;
    }

    /**
     * Return the S3 key for the stream's info field.
     */
    public String streamInfoKey () {
        /* stream.<encoded stream name>.info */
        return STREAM_PREFIX + FIELD_DELIMETER + _encodedStreamName +
            FIELD_DELIMETER + INFO_FIELD;
    }


    /**
     * Return the S3 key for the stream's corresponding data block.
     */
    public String streamBlockKey (long blockId) {
        /* stream.<encoded stream name>.block.<blockId> */
        return STREAM_PREFIX + FIELD_DELIMETER + _encodedStreamName +
            FIELD_DELIMETER + BLOCK_FIELD + FIELD_DELIMETER +
            Long.toString(blockId);
    }

    /**
     * Return the stream name.
     */
    public String getStreamName ()
    {
        return _streamName;
    }

    /** S3 Connection. */
    private final S3Connection _connection;
    
    /** S3 Bucket. */
    private final String _bucketName;

    /** Stream name. */
    private final String _streamName;

    /** Stream encoded name. */
    private final String _encodedStreamName;

    /** Data structure version. Used to support backwards compatibility*/
    protected static final int VERSION = 1;

    /** Key to stream name. */
    private static final String INFO_KEY_NAME = "name";

    /** Key to stream version. */
    private static final String INFO_KEY_VERSION = "version";

    /** Key to stream creation date. */
    private static final String INFO_KEY_CTIME = "ctime";

    /** Stream prefix. All stream-related keys will be prepended with this
      * prefix. */
    private static final String STREAM_PREFIX = "stream";

    /** Field delimiter. */
    private static final String FIELD_DELIMETER = ".";

    /** Block data field. */
    private static final String BLOCK_FIELD = "block";

    /** Info data field. */
    private static final String INFO_FIELD = "info";

    /** Character set encoding used for base64'd stream names. */
    private static final String NAME_ENCODING = "utf-8";
}
