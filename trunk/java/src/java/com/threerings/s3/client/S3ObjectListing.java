/* 
 * S3ObjectListing vi:ts=4:sw=4:expandtab:
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

import com.threerings.s3.client.xml.S3ObjectListingHandler;

import java.io.InputStream;
import java.io.IOException;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.SimpleTimeZone;

import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;


/**
 * Returned by S3Connection.listBucket()
 */
public class S3ObjectListing {

    public S3ObjectListing (InputStream dataStream)
        throws IOException, SAXException
    {
        /* Configure our SAX parser. */
        XMLReader xr = XMLReaderFactory.createXMLReader();
        S3ObjectListingHandler handler = new S3ObjectListingHandler();
        xr.setContentHandler(handler);
        xr.setErrorHandler(handler);

        /* Parse the XML. Will throw a SAXException if it fails. */
        xr.parse(new InputSource(dataStream));            

        /* Fetch the newly parsed data. */
        _bucketName = handler.getBucketName();
        _prefix = handler.getPrefix();
        _marker = handler.getMarker();
        _delimiter = handler.getDelimiter();
        _maxKeys = handler.getMaxKeys();
        _truncated = handler.getTruncated();
        _nextMarker = handler.getNextMarker();
        _entries = handler.getObjectEntries();
        _commonPrefixes = handler.getCommonPrefixes();
    }
    

    public String getBucketName () {
        return _bucketName;
    }

    public String getPrefix () {
        return _prefix;
    }

    public String getMarker () {
        return _marker;
    }

    public String getDelimiter () {
        return _delimiter;
    }

    public int getMaxKeys () {
        return _maxKeys;
    }

    public boolean getTruncated () {
        return _truncated;
    }

    public String getNextMarker () {
        return _nextMarker;
    }

    public List<S3ObjectEntry> getEntries () {
        return _entries;
    }

    public List<String> getCommonPrefixes () {
        return _commonPrefixes;
    }


    /** The name of the bucket being listed. */
    private String _bucketName;

    /** The prefix echoed back from the request. Null if not specified in the
     * request */
    private String _prefix = null;

    /** The marker echoed back from the request. Null if not specified in the
     * request. */
    private String _marker = null;

    /** The request delimiter (echoed from the request). Null if not specified
     * in the request. */
    private String _delimiter = null;

    /** The maximum number of returned keys (echoed from the request). 0 if not
     * specified. */
    private int _maxKeys = 0;

    /** Indicates if there are more results to the list.  True if the current
     * list results have been truncated. */
    private boolean _truncated;

    /** Indicates what to use as a marker for subsequent list requests in the
     * event that the results are truncated.  Present only when a delimiter is
     * specified. (XXX Is the delimiter comment actually true?) */
    private String _nextMarker = null;

    /** The list of object entries. */  
    private List<S3ObjectEntry> _entries;

    /** A List of prefixes representing the common prefixes of the keys that
     * matched up to the delimiter. */
    private List<String> _commonPrefixes;
}
