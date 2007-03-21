//  This software code is made available "AS IS" without warranties of any
//  kind.  You may copy, display, modify and redistribute the software
//  code either by itself or as incorporated into your code; provided that
//  you do not remove any proprietary notices.  Your use of this software
//  code is at your own risk and you waive any claim against Amazon
//  Digital Services, Inc. or its affiliates with respect to your use of
//  this software code.
//
// (c) 2006 Three Rings Design, Inc.
//

package com.threerings.s3.client;

/** 
 * An exception that indicates a generic client-side S3 error.
 */
public class S3Exception extends Exception
{
    public S3Exception (String message) {
        super(message);
    }

    public S3Exception (String message, Throwable cause) {
        super(message, cause);
    }
}