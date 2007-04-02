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

package com.threerings.s3.client.xml;

import com.threerings.s3.client.S3ObjectEntry;
import com.threerings.s3.client.S3Owner;

import junit.framework.TestCase;

import java.io.StringReader;
import java.util.List;

import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;

public class S3ObjectListingHandlerTest extends TestCase
{
    public S3ObjectListingHandlerTest (String name)
    {
        super(name);
    }
    
    public void setUp ()
        throws Exception
    {

    }
    
    public void tearDown ()
        throws Exception
    {
    }


    public void testParse ()
        throws Exception
    {
        /* Configure our SAX parser. */
        XMLReader xr = XMLReaderFactory.createXMLReader();
        S3ObjectListingHandler h = new S3ObjectListingHandler();
        xr.setContentHandler(h);
        xr.setErrorHandler(h);

        /* Parse the XML. Will throw a SAXException if it fails. */
        xr.parse(new InputSource(new StringReader(TEST_DATA)));

        /* Validate the result. */
        assertEquals("test-bucket", h.getBucketName());
        assertEquals("aPrefix", h.getPrefix());
        assertEquals("aMarker", h.getMarker());
        assertEquals(1000, h.getMaxKeys());
        assertEquals(false, h.getTruncated());

        List<S3ObjectEntry> entries;
        List<String> prefixes;
        S3ObjectEntry entry;
        S3Owner owner;

        entries = h.getObjectEntries();
        assertEquals(1, entries.size());
        
        prefixes = h.getCommonPrefixes();
        assertEquals(1, prefixes.size());

        entry = entries.get(0);
        owner = entry.getOwner();

        assertEquals("aKey", entry.getKey());
        assertEquals("65a8e27d8879283831b664bd8b7f0ad4", entry.getETag());
        assertEquals(13, entry.getSize());
        assertEquals("STANDARD", entry.getStorageClass());
        assertEquals(1175540488000L, entry.getLastModified().getTime());
        
        assertEquals("a59a930aa2b83ac8f7164e4a541d4d5f67ef3c87750011cc82bda1f5827d1ebd", owner.getId());
        assertEquals("landonfuller", owner.getDisplayName());
        
        assertEquals("/foobar", prefixes.get(0));
    }

    /** Test XML Response Data. */
    protected static final String TEST_DATA =
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">\n" +
"   <Name>test-bucket</Name>\n" +
"   <Prefix>aPrefix</Prefix>\n" +
"   <Marker>aMarker</Marker>\n" +
"   <MaxKeys>1000</MaxKeys>\n" +
"   <IsTruncated>false</IsTruncated>\n" +
"   <Contents>\n" +
"       <Key>aKey</Key>\n" +
"       <LastModified>2007-04-02T19:01:28.000Z</LastModified>\n" + // Microseconds since epoch: 1175540488000
"       <ETag>&quot;65a8e27d8879283831b664bd8b7f0ad4&quot;</ETag>\n" +
"       <Size>13</Size>\n" +
"       <Owner>\n" +
"           <ID>a59a930aa2b83ac8f7164e4a541d4d5f67ef3c87750011cc82bda1f5827d1ebd</ID>\n" +
"           <DisplayName>landonfuller</DisplayName>\n" +
"       </Owner>\n" +
"       <StorageClass>STANDARD</StorageClass>\n" +
"   </Contents>\n" +
"   <CommonPrefixes>\n" +
"       <Prefix>/foobar</Prefix>\n" +
"   </CommonPrefixes>\n" +
"</ListBucketResult>\n";
}
