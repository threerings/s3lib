/* 
 * S3ObjectListingHandler vi:ts=4:sw=4:expandtab:
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

package com.threerings.s3.client.xml;

import com.threerings.s3.client.S3ObjectEntry;
import com.threerings.s3.client.S3Owner;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.SimpleTimeZone;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;

/**
 * Parses S3 GET bucket XML responses. The responses have no schema/DTD and thus
 * can not be properly validated, thus, malformed input may result in invalid output,
 * possibly without reporting an error.
 * See http://docs.amazonwebservices.com/AmazonS3/2006-03-01/ListingKeysResponse.html
 *
 * @todo Validate max-keys, and restrict total data read, to ensure that a remote server
 * can't fill our heap with a result set.
 */
public class S3ObjectListingHandler extends DefaultHandler {
    public S3ObjectListingHandler () {
        /* Initialize the time parser. */
        _timeParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        _timeParser.setTimeZone(new SimpleTimeZone(0, "GMT"));
        _text = new StringBuffer();
    }


    public String getBucketName () {
        return _data.bucketName;
    }

    public String getPrefix () {
        return _data.prefix;
    }

    public String getMarker () {
        return _data.marker;
    }

    public String getDelimiter () {
        return _data.delimiter;
    }

    public int getMaxKeys () {
        return _data.maxKeys;
    }

    public boolean getTruncated () {
        return _data.truncated;
    }

    public String getNextMarker () {
        return _data.nextMarker;
    }

    public List<S3ObjectEntry> getObjectEntries () {
        return _data.entries;
    }

    public List<String> getCommonPrefixes () {
        return _data.commonPrefixes;
    }

    @Override
    public void startDocument ()
        throws SAXException
    {
        _depth = 0;
        _state = new Stack<ParserState>();
        _state.push(ParserState.DEFAULT);
    }

    @Override
    public void endDocument ()
        throws SAXException
    {
        /* Pop our final state. If this assert fails, there's a critical
         * bug in the parser. */
        ParserState state = _state.pop();
        assert(state == ParserState.DEFAULT);

        /* Validate the data */
        _data.validate();
    }

    @Override
    public void startElement (String uri, String name, String qName, Attributes attrs)
        throws SAXException
    {
        /* Reset the element text buffer. */
        if (_text.length() != 0) {
            _text = new StringBuffer();            
        }

        /*
         * We maintain a state stack, and a switch() based state machine.
         * How very C of us.
         * Unless we're switching to a new state, we fall through and let the
         * endElement handler deal with any element data.
         */
        switch (_state.peek()) {
            case DEFAULT:
                /* State transitions. */
                if (name.equals("ListBucketResult")) {
                    _state.push(ParserState.LISTBUCKET);
                    break;
                } else {
                    throw new UnsupportedElementException(name);                
                }
            case LISTBUCKET:
                /* State transitions. */
                if (name.equals("Contents")) {
                    /* <Contents> section */
                    _state.push(ParserState.CONTENTS);
                    
                    /* Initialize the current entry. We'll fill this out in the
                     * endElement handler. */
                    _data.entry = _data.new Entry();

                } else if (name.equals("CommonPrefixes")) {
                    /* <CommonPrefixes> section */
                    _state.push(ParserState.PREFIXES);
                }
                break;
            case CONTENTS:
                /* State transitions. */
                if (name.equals("Owner")) {
                    _state.push(ParserState.OWNER);
                }
                break;
            default:
                /* No state transition, let the endElement handler deal with it. */
                break;
        }

        /* Increment the element depth. */
        _depth++;
    }

    @Override
    public void endElement (String uri, String name, String qName)
        throws SAXException
    {
        switch (_state.peek()) {
            case DEFAULT:
                // Top-level. Anything to do?
                break;
            case LISTBUCKET:
                /* Handle the exit state. */
                if (name.equals("ListBucketResult")) {
                    _state.pop();
                    break;
                }
            
                /* Handle the data elements. */
                if (name.equals("Name")) {
                    _data.bucketName = getElementString();
                }

                else if (name.equals("Prefix")) {
                    _data.prefix = getElementString();
                }

                else if (name.equals("Marker")) {
                    _data.marker = getElementString();
                }

                else if (name.equals("MaxKeys")) {
                    _data.maxKeys = getElementInteger();                        
                }

                else if (name.equals("IsTruncated")) {
                    _data.truncated = getElementBoolean();
                }

                else if (name.equals("NextMarker")) {
                    _data.nextMarker = getElementString();
                }

                else if (name.equals("Delimiter")) {
                    _data.delimiter = getElementString();
                }

                else {
                    throw new UnsupportedElementException(name);
                }

                break;

            case CONTENTS:
                /* Handle the exit state. */
                if (name.equals("Contents")) {
                    _state.pop();
                    
                    /* Validate the data and add the S3ObjectEntry to the list of entries. */
                    _data.entries.add(_data.entry.validate());

                    _data.entry = null;
                    break;
                }

                /* Handle the data elements. */
                if (name.equals("Key")) {
                    _data.entry.key = getElementString();
                }
                
                else if (name.equals("LastModified")) {
                    _data.entry.lastModified = getElementDate();
                }

                else if (name.equals("ETag")) {
                    /* ETags are in "hex checksum" format. We strip the leading
                     * and trailing " */
                    String value = getElementString();
                    _data.entry.eTag = value.substring(1, value.length() - 1);
                }

                else if (name.equals("Size")) {
                    _data.entry.size = getElementLongInteger();
                }
                
                else if (name.equals("StorageClass")) {
                    _data.entry.storageClass = getElementString();
                }
                
                else {
                    throw new UnsupportedElementException(name);
                }

                break;

            case OWNER:
                /* Handle the exit state. */
                if (name.equals("Owner")) {
                    _state.pop();
                    break;
                }

                /* Handle the data elements. */
                if (name.equals("ID")) {
                    _data.entry.ownerId = getElementString();
                }
                
                else if (name.equals("DisplayName")) {
                    _data.entry.ownerDisplayName = getElementString();
                }
                
                else {
                    throw new UnsupportedElementException(name);
                }

                break;

            case PREFIXES:
                /* Handle the exit state. */
                if (name.equals("CommonPrefixes")) {
                    _state.pop();
                    break;
                }

                /* Handle the data elements. */
                if (name.equals("Prefix")) {
                    _data.commonPrefixes.add(getElementString());
                } else {
                    throw new UnsupportedElementException(name);
                }
                break;
        }

        /* Decrement the element depth. */
        _depth--;
    }

    @Override
    public void characters(char ch[], int start, int length)
        throws SAXException
    {
        /* Sanity check the element text length */
        if (_text.length() + ch.length > 16384) {
            throw new SAXException("Ridiculously large XML text field.");
        }

        /* Append the new characters to the text buffer. */
        _text.append(ch, start, length);
    }

    /** Retrieve the current element's text value. */
    protected String getElementString ()
        throws SAXException
    {
        return _text.toString();
    }

    /** Retrieve the current element's integer value. */
    protected int getElementInteger ()
        throws SAXException
    {
        try {
            return Integer.parseInt(_text.toString());
        } catch (NumberFormatException nfe) {
            throw new SAXException("Error parsing integer: " + _text.toString(), nfe);
        }
    }

    /** Retrieve the current element's long integer value. */
    protected long getElementLongInteger ()
        throws SAXException
    {
        try {
            return Long.parseLong(_text.toString());
        } catch (NumberFormatException nfe) {
            throw new SAXException("Error parsing long integer: " + _text.toString(), nfe);
        }
    }

    /** Retrieve the current element's boolean value. */
    protected boolean getElementBoolean ()
        throws SAXException
    {
        /* Will not throw an exception, simply returns false. */
        return Boolean.parseBoolean(_text.toString());
    }

    /** Retrieve the current element's (ISO 8601) Date value. */
    protected Date getElementDate ()
        throws SAXException
    {
        try {
            return _timeParser.parse(_text.toString());            
        } catch (ParseException pe) {
            throw new SAXException("Unable to parse date: " + _text.toString(), pe);
        }
    }


    /** Parsed document state. */
    private class ListingData {
        /** ListBucketResult.Name */
        public String bucketName;

        /** ListBucketResult.Prefix */
        public String prefix;

        /** ListBucketResult.Marker */
        public String marker;

        /** ListBucketResult.MaxKeys */
        public int maxKeys;

        /** ListBucketResult.Delimiter */
        public String delimiter;

        /** ListBucketResult.IsTruncated */
        public boolean truncated;

        /** ListBucketResult.NextMarker */
        public String nextMarker;

        /** All ListBucketResult.Contents */
        public List<S3ObjectEntry> entries = new ArrayList<S3ObjectEntry>();

        /** All ListBucketResult.CommonPrefixes */
        public List<String> commonPrefixes = new ArrayList<String>();

        /** Current ListBucketResult.Contents entry */
        public Entry entry;

        public void validate ()
            throws MissingElementException
        {
            if (bucketName == null) {
                throw new MissingElementException("Name");
            }
        }

        /**
         * A structure representing a Contents entry stored in S3.
         */
        public class Entry {
            /** Key. */
            public String key;

            /** LastModified. */
            public Date lastModified;

            /** ETag */
            public String eTag;

            /** Size */
            public long size = -1;

            /** StorageClass */
            public String storageClass;

            /** Owner.ID */
            public String ownerId;

            /** Owner.DisplayName */
            public String ownerDisplayName;

            /** Validate the data and create the new S3Entry class. */
            public S3ObjectEntry validate ()
                throws MissingElementException
            {
                if (key == null) {
                    throw new MissingElementException("Key");
                }

                if (lastModified == null) {
                    throw new MissingElementException("LastModified");
                }

                if (eTag == null) {
                    throw new MissingElementException("ETag");
                }

                if (size == -1) {
                    throw new MissingElementException("Size");
                }

                if (ownerId == null) {
                    throw new MissingElementException("Owner ID");
                }

                /* Display name might be missing. If so, we provide the ownerId */
                if (ownerDisplayName == null) {
                    ownerDisplayName = ownerId;
                }

                S3Owner owner = new S3Owner(ownerId, ownerDisplayName);
                return new S3ObjectEntry(key, lastModified, eTag, size, storageClass, owner);
            }
        }
    }

    /** A state for each enclosing element name */
    private static enum ParserState {
        DEFAULT,    /* Top level */
        LISTBUCKET, /* ListBucketResult */
        CONTENTS,   /* Contents */
        OWNER,      /* Owner */
        PREFIXES,   /* CommonPrefixes */
    }

    /** Document data. */
    private ListingData _data = new ListingData();

    /** Current parser state stack. */
    private Stack<ParserState> _state;

    /** Current element depth. */
    private int _depth;

    /** Current element text contents. */
    private StringBuffer _text = null;

    /** ISO 8601 Date Parser. */
    private SimpleDateFormat _timeParser = null;
}