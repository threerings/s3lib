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

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.HashMap;
import java.util.Map;

import com.threerings.s3.client.acl.AccessControlList;
import com.threerings.s3.client.S3ByteArrayObject;
import com.threerings.s3.client.S3Connection;
import com.threerings.s3.client.S3Exception;
import com.threerings.s3.client.S3Object;
import com.threerings.s3.client.S3ServerException;

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
        throws RemoteStreamException, IOException, S3Exception
    {
        Map<String,String> metadata = new HashMap<String,String>();
        S3ByteArrayObject infoObject = new S3ByteArrayObject(streamInfoKey(), new byte[0], S3Object.DEFAULT_MIME_TYPE);

        /* Set the version. */
        metadata.put(INFO_KEY_VERSION, Integer.toString(VERSION));

        /* Upload the info object. */
        infoObject.setMetadata(metadata);
        _connection.putObject(_bucketName, infoObject, AccessControlList.StandardPolicy.PRIVATE);
    }


    public RemoteStreamInfo getStreamInfo ()
        throws RemoteStreamException, IOException, S3Exception
    {
        S3Object object;
        Map<String,String> metadata;
        String versionString = "";
        int version;

        try {
            /* Fetch the stream's info record. */
            object = _connection.getObject(_bucketName, streamInfoKey());
            metadata = object.getMetadata();
            versionString = metadata.get(INFO_KEY_VERSION);

            if (versionString == null) {
                throw new RemoteStreamException.UnsupportedVersionException(
                    "Stream missing version number");
            }
            
            version = Integer.parseInt(versionString);
            return new RemoteStreamInfo(version);

        } catch (S3ServerException.NoSuchKeyException nsk) {
            throw new RemoteStreamException.StreamNotFoundException("No such stream (" + _streamName + ")");
        } catch (NumberFormatException nfe) {
            throw new RemoteStreamException.UnsupportedVersionException("Unsupported version: " + versionString);
        }
    }


    /**
     * Returns true if the stream exists on the remote server.
     * False otherwise.
     */
    public boolean exists ()
        throws RemoteStreamException
    {
        try {
            getStreamInfo();
            return true;
        } catch (RemoteStreamException.StreamNotFoundException snf) {
            return false;
        } catch (S3Exception s3e) {
            throw new RemoteStreamException.ServiceException("S3 failure: " + s3e.getMessage(), s3e);
        } catch (IOException ioe) {
            throw new RemoteStreamException.ServiceException("Network failure: " + ioe.getMessage(), ioe);            
        }
    }

    /**
     * Return the S3 key for a given encoded stream name's info field.
     */
    public String streamInfoKey () {
        /* stream.<encoded stream name>.info */
        return STREAM_PREFIX + FIELD_DELIMETER + _encodedStreamName +
            FIELD_DELIMETER + INFO_FIELD;
    }


    /**
     * Return the S3 key for a given encoded stream name and block id.
     */
    public String streamBlockKey (long blockId) {
        /* stream.<encoded stream name>.block.<blockId> */
        return STREAM_PREFIX + FIELD_DELIMETER + _encodedStreamName +
            FIELD_DELIMETER + BLOCK_FIELD + FIELD_DELIMETER +
            Long.toString(blockId);
    }


    /** S3 Connection. */
    private S3Connection _connection;
    
    /** S3 Bucket. */
    private String _bucketName;

    /** Stream name. */
    private String _streamName;

    /** Stream encoded name. */
    private String _encodedStreamName;

    /** Data structure version. Used to support backwards compatibility*/
    protected static final int VERSION = 1;

    /** Key to info version metadata. */
    private static final String INFO_KEY_VERSION = "version";

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
