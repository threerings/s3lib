//  This software code is made available "AS IS" without warranties of any
//  kind.  You may copy, display, modify and redistribute the software
//  code either by itself or as incorporated into your code; provided that
//  you do not remove any proprietary notices.  Your use of this software
//  code is at your own risk and you waive any claim against Amazon
//  Digital Services, Inc. or its affiliates with respect to your use of
//  this software code. (c) 2006 Amazon Digital Services, Inc. or its
//  affiliates.

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

    /** The name of the bucket being listed. */
    public String name;

    /** The prefix echoed back from the request. */
    public String prefix;

    /** The marker echoed back from the request. */
    public String marker;

    /** The request delimiter (echoed from the request). Null if not specified
     * in the request. */
    public String delimiter = null;

    /** The maximum number of returned keys (echoed from the request). 0 if not
     * specified. */
    public int maxKeys = 0;

    /** Indicates if there are more results to the list.  True if the current
     * list results have been truncated. */
    public boolean isTruncated;

    /** Indicates what to use as a marker for subsequent list requests in the
     * event that the results are truncated.  Present only when a delimiter is
     * specified. (XXX Is the delimiter comment actually true?) */
    public String nextMarker = null;

    /** The list of object entries. */  
    public List<S3ObjectEntry> entries;

    /** A List of prefixes representing the common prefixes of the keys that
     * matched up to the delimiter. */
    public List<String> commonPrefixes;


    public S3ObjectListing (InputStream dataStream)
        throws IOException, SAXException
    {
        XMLReader xr = XMLReaderFactory.createXMLReader();
        S3ObjectListingHandler handler = new S3ObjectListingHandler();
        xr.setContentHandler(handler);
        xr.setErrorHandler(handler);

        xr.parse(new InputSource(dataStream));
/*
        this.name = handler.getName();
        this.prefix = handler.getPrefix();
        this.marker = handler.getMarker();
        this.delimiter = handler.getDelimiter();
        this.maxKeys = handler.getMaxKeys();
        this.isTruncated = handler.getIsTruncated();
        this.nextMarker = handler.getNextMarker();
        this.entries = handler.getKeyEntries();
        this.commonPrefixes = handler.getCommonPrefixes();
*/
    }
    
    /*
    public String getName () {
        return this.name;
    }

    public String getPrefix () {
        return this.prefix;
    }

    public String getMarker () {
        return this.marker;
    }

    public String getDelimiter () {
        return this.delimiter;
    }

    public int getMaxKeys () {
        return this.maxKeys;
    }

    public boolean getIsTruncated () {
        return this.isTruncated;
    }

    public String getNextMarker () {
        return this.nextMarker;
    }

    public List<S3ObjectEntry> getKeyEntries () {
        return this.keyEntries;
    }

    public List<String> getCommonPrefixes () {
        return this.commonPrefixes;
    }
    */
}
