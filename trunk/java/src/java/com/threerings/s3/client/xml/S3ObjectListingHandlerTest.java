/* 
 * S3ObjectListingHandlerTest vi:ts=4:sw=4:expandtab:
 *
 * Copyright (c) 2005 - 2007 Three Rings Design, Inc.
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
