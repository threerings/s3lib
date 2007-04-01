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
