/*
 * Author: Landon Fuller <landonf@plausiblelabs.com>
 *
 * Copyright (c) 2009 Plausible Labs Cooperative, Inc.
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

package com.plausiblelabs.s3.client

import com.threerings.s3.client.acl.AccessControlList.StandardPolicy


/**
 * Represents a heirarchical key prefix space in Amazon S3, using the UNIX standard
 * "/" path delimiter.
 *
 * S3 does not natively support UNIX-style directory paths -- all object key names are
 * wholly arbitrary. However, prefix-limited and suffix-limited search may be used
 * to emulate path-style directory access. This class provides simple management of a
 * such a prefix/delimiter key-space.
 * 
 * While it is possible to use an arbitrary character for S3 key delimeter,
 * the UNIX-style / has the advantage of being standardized across multiple
 * protocols and operating systems, and thus is the only supported delimiter.
 *
 * For further reading, refer to
 * http://docs.amazonwebservices.com/AmazonS3/2006-03-01/ListingKeysHierarchy.html
 *
 * @param bucket Bucket containing this path prefix.
 * @param name Path name. (eg, "/documents/"). This path will be normalized according
 * to the standard UNIX path normalization rules. Multiple sequential delimiters will
 * be collapsed, and references to '..' will be resolved.
 */
class Path (bucket:Bucket, name:String) extends S3Storage {
  /** Normalized path */
  private val path = Normalize.path(name)

  override def subpath (name:String) = new Path(bucket, Normalize.subpath(path, name))

  // from S3Storage trait
  override def put (key:String, obj:S3Object, policy:StandardPolicy) =
    bucket.put(Normalize.subpath(path, key), obj, policy)

  // from S3Storage trait
  override def delete (key:String) = bucket.delete(Normalize.subpath(path, key))

  // from S3Storage trait
  override def get (key:String) = bucket.get(Normalize.subpath(path, key))
}
