//  This software code is made available "AS IS" without warranties of any
//  kind.  You may copy, display, modify and redistribute the software
//  code either by itself or as incorporated into your code; provided that
//  you do not remove any proprietary notices.  Your use of this software
//  code is at your own risk and you waive any claim against Amazon
//  Digital Services, Inc. or its affiliates with respect to your use of
//  this software code.

// (c) 2007 Three Rings Design, Inc.

package com.threerings.s3.client.xml;

import org.xml.sax.SAXException;

/**
 * Unsupported element found -- poor man's validation.
 * This would be unnecessary if Amazon published DTD/Schema.
 */
class UnsupportedElementException extends SAXException {
    protected UnsupportedElementException (String name) {
        super("Unsupported XML element found: " + name);
    }
}
