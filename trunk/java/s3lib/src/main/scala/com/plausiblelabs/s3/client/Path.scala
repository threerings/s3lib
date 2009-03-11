/*
 * Author: Landon Fuller <landonf@plausiblelabs.com>
 *
 * Copyright (c) 2009 Plausible Labs Cooperative, Inc.
 * All rights reserved.
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

  /**
   * Return a new sub-path. The requested path will be normalized, appended to this
   * path and returned.
   *
   * @param name The path name to append.
   */
  def subpath (name:String) = new Path(bucket, Normalize.subpath(path, name))

  // from S3Storage trait
  def put (key:String, obj:S3Object, policy:StandardPolicy) =
    bucket.put(Normalize.subpath(path, key), obj, policy)

  // from S3Storage trait
  def delete (key:String) = bucket.delete(Normalize.subpath(path, key))

  // from S3Storage trait
  def get (key:String) = bucket.get(Normalize.subpath(path, key))
}
