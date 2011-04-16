package com.threerings.s3.client;

import java.util.HashMap;
import java.util.Map;

/**
 * A representation of the metadata of a single object stored in S3.
 */
public abstract class S3Metadata {
    /** Default binary media type. */
    public static final MediaType DEFAULT_MEDIA_TYPE = new MediaType("binary/octet-stream");

    /**
     * Instantiate an S3 object's metadata with the given key.
     * @param key S3 object key.
     */
    public S3Metadata (String key)
    {
        this(key, DEFAULT_MEDIA_TYPE);
    }

    /**
     * Instantiate an S3 Object's metadata with the given key and media type.
     * @param key S3 object key.
     * @param mediaType Object's media type.
     */
    public S3Metadata (String key, MediaType mediaType)
    {
        this(key, mediaType, new HashMap<String,String>());
    }

    /**
     * Instantiate an S3 Object's metadata with the given key, media type, and initial set of
     * metadata.
     * @param key S3 object key.
     * @param mediaType Object's media type.
     * @param metadata Object's metadata. Metadata keys must be a single, ASCII string, and may
     * not contain spaces. Metadata values must also be ASCII, and any leading or trailing spaces
     * may be stripped.
     */
    public S3Metadata (String key, MediaType mediaType, Map<String,String> metadata)
    {
        _key = key;
        _mediaType = mediaType;
        _metadata = metadata;
    }

    /**
     * Returns the S3 Object Key.
     */
    public String getKey ()
    {
        return _key;
    }

    /**
     * Return the S3 Object's media type
     */
    public MediaType getMediaType () {
        return _mediaType;
    }

    /**
     * Returns the S3 Object's metadata.
     */
    public Map<String,String> getMetadata ()
    {
        return _metadata;
    }

    /**
     * Set the S3 Object's metadata.
     * Metadata keys must be a single, ASCII string, and may not contain spaces.
     */
    public void setMetadata (Map<String,String> metadata)
    {
        _metadata = metadata;
    }

    /**
     * Return the object's modification timestamp, or 0L if the timestamp
     * is unknown or can not be read.
     */
    public long lastModified () {
        return 0L;
    }

    /**
     * Get the object's MD5 checksum. If the checksum is unavailable,
     * this method may return null.
     */
    public abstract byte[] getMD5 () throws S3ClientException;

    /**
     * Returns the number of bytes required to store the S3 Object.
     */
    public abstract long length ();

    /** S3 object media type. */
    private MediaType _mediaType;

    /** S3 object name. */
    private String _key;

    /** S3 object meta-data. */
    private Map<String,String> _metadata;
}
