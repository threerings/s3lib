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

import com.threerings.s3.client.S3Exception

import com.threerings.s3.client.S3ObjectEntry

import com.threerings.s3.client.{S3Object => JS3Object, MediaType => JMediaType}
import com.threerings.s3.client.S3ObjectListing
import com.threerings.s3.client.acl.AccessControlList.StandardPolicy

import scala.collection.JavaConversions._


/**
 * Wraps java and scala S3Object instances.
 */
private[client] object S3ObjectWrapper {

  private def nullable[T] (value:T) = value match {
    case null => None
    case value => Some(value)
  }

  /**
   * Wraps Scala S3 objects, providing a java client API-compatible
   * interface
   */
  private[client] class Scala (key:String, obj:S3Object)
    extends JS3Object (key, new JMediaType(obj.mediaType.mimeType, obj.mediaType.contentEncoding.getOrElse(null)))
  {
    override def lastModified = obj.lastModified match {
      case None => 0
      case Some(d) => d.getTime
    }

    override def getInputStream = obj.inputStream
    override def getMD5 = obj.md5.getOrElse(null)
    override def length:Long = obj.length.getOrElse(-1)
  }

  /**
   * Wraps Scala S3 objects, providing a java client API-compatible
   * interface
   */
  private[client] class Java (key:String, obj:JS3Object) extends S3Object {
    import java.util.Date
    import java.io.InputStream

    override def mediaType:MediaType = {
      val jmedia = obj.getMediaType
      new MediaType(jmedia.getMimeType, nullable(jmedia.getContentEncoding))
    }

    override def lastModified:Option[Date] = obj.lastModified match {
      case 0 => None
      case d:Long => Some(new Date(d))
    }

    override def inputStream:InputStream = obj.getInputStream
    override def md5:Option[Array[Byte]] = nullable(obj.getMD5)
    override def length:Option[Long] = obj.length match {
      case -1 => None
      case len => Some(len)
    }
  }
}

/**
 * Generic S3 storage.
 *
 * A specific storage instance (such as Path) may permute the keyspace or
 * otherwise provide additional functionality on top of the base
 * Bucket storage class.
 */
trait S3Storage {
  /**
   * Return a new sub-path. The requested path will be normalized, appended to the
   * curren path (if any) and returned.
   *
   * @param name The path name to append.
   */
  def subpath (name:String): S3Storage

  /**
   * Fetch an S3 object with the provided key.
   *
   * @param key Object key.
   */
  def get (key:String): S3Object

  /**
   * Upload an S3 object using a StandardPolicy.PRIVATE access
   * policy.
   *
   * @param key S3 object key. Note that the final key name
   * will be relative to any path prefix or permutation used by
   * this storage item.
   * @param obj Object to upload.
   */
  def put (key:String, obj:S3Object): Unit = {
    this.put(key, obj, StandardPolicy.PRIVATE)
  }

  /**
   * Upload an S3 object.
   *
   * @param key S3 object key. Note that the final key name
   * will be relative to any path prefix or permutation used by
   * this storage item.
   * @param obj Object to upload.
   * @param policy Access policy to use for the uploaded item.
   */
  def put (key:String, obj:S3Object, policy:StandardPolicy): Unit

  /**
   * Delete a remote S3 Object.
   * @param key S3 object key.
   */
  def delete (key:String)
}

/**
 * Represents a remote S3 bucket.
 *
 * @param name Bucket name.
 * @param account S3 account.
 */
class Bucket (val name:String, account:S3Account) extends S3Storage {
  /** Backing S3 connection */
  private val conn = account.conn

  /**
   * Create the S3 bucket.
   */
  @throws(classOf[S3Exception])
  def create = account.retry(conn.createBucket(name))


  /**
   * Delete the S3 bucket.
   */
  @throws(classOf[S3Exception])
  def delete = account.retry(conn.deleteBucket(name))


  // from S3Storage trait
  def subpath (path:String): S3Storage = new Path(this, path)


  // from S3Storage trait
  def get (key:String): S3Object =
    new S3ObjectWrapper.Java(key, account.retry(conn.getObject(name, key)))


  // from S3Storage trait
  def put (key:String, obj:S3Object, policy:StandardPolicy): Unit =
    account.retry(conn.putObject(name, new S3ObjectWrapper.Scala(key, obj), policy))


  // from S3Storage trait
  def delete (key:String) =
    account.retry(conn.deleteObject(name, key))


  /**
   * Return a stream of all bucket objects.
   */
  @throws(classOf[S3Exception])
  def objects: Stream[S3ObjectEntry] = {
    objects(null, null, null)
  }


  /**
   * Return a stream of all bucket objects
   *
   * @param prefix Limits response to keys beginning with the provided prefix.
   * @param delimiter Keys that contain the same string between the prefix and the first
   * occurence of the delimiter will be rolled up into a single result element in the CommonPrefixes data.
   */
  @throws(classOf[S3Exception])
  def objects (prefix:String, delimiter:String): Stream[S3ObjectEntry] = {
    objects(prefix, delimiter, null)
  }


  /**
   * Return a stream of all bucket objects.
   *
   * @param prefix Limits response to keys beginning with the provided prefix.
   * @param delimiter Keys that contain the same string between the prefix and the first
   * occurence of the delimiter will be rolled up into a single result element in the CommonPrefixes data.
   * @param marker Indicates where in the bucket to begin listing. The list will only include keys that
   * occur lexicographically after the marker.
   */
  @throws(classOf[S3Exception])
  def objects (prefix:String, delimiter:String, marker:String): Stream[S3ObjectEntry] = {
    objects(prefix, delimiter, marker, 0)
  }


  /**
   * Return a stream of all bucket objects
   *
   * @param prefix Limits response to keys beginning with the provided prefix.
   * @param delimiter Keys that contain the same string between the prefix and the first
   * occurence of the delimiter will be rolled up into a single result element in the CommonPrefixes data.
   * @param marker Indicates where in the bucket to begin listing. The list will only include keys that
   * occur lexicographically after the marker.
   * @param maxKeys Maximum number of keys to return. The server may return fewer keys, but never more.
   */
  @throws(classOf[S3Exception])
  private[client] def objects (prefix:String, delimiter:String, marker:String, maxObjects:Int): Stream[S3ObjectEntry] = {
    /* Generate an entry stream */
    def generate (marker:String) = {
      val listing = conn.listObjects(name, prefix, marker, maxObjects, delimiter)
      val entries = listing.getEntries
      if (entries.isEmpty) {
        Stream.empty
      } else {
        val s = entries.toStream
        Stream.cons(s.head, next(listing, s.tail))
      }
    }

    /* Return the next stream */
    def next (prev:S3ObjectListing, stream:Stream[S3ObjectEntry]): Stream[S3ObjectEntry] = {
      if (stream.isEmpty) {
        if (prev.truncated) {
          generate(prev.getNextMarker)
        } else {
          Stream.empty
        }
      } else {
        Stream.cons(stream.head, next(prev, stream.tail))
      }
    }

    generate(marker)
  }
}
