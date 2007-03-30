//  This software code is made available "AS IS" without warranties of any
//  kind.  You may copy, display, modify and redistribute the software
//  code either by itself or as incorporated into your code; provided that
//  you do not remove any proprietary notices.  Your use of this software
//  code is at your own risk and you waive any claim against Amazon
//  Digital Services, Inc. or its affiliates with respect to your use of
//  this software code. (c) 2006 Amazon Digital Services, Inc. or its
//  affiliates.

package com.threerings.s3.client;

/**
 * A structure representing the Amazon Web Services owner of an object.
 */
public class S3Owner {

    public S3Owner (String id, String displayName) {
        _id = id;
        _displayName = displayName;
    }

    public String getId () {
        return _id;
    }

    public String getDisplayName () {
        return _displayName;
    }

    /** Canonical AWS ID. */
    protected String _id;

    /** AWS display name. */
    protected String _displayName;
}
