/*
 * Author: Landon Fuller <landonf@plausiblelabs.com>
 *
 * Copyright (c) 2009 Plausible Labs Cooperative, Inc.
 * All rights reserved.
 */

package com.plausiblelabs.s3.client

import com.threerings.s3.client.S3Exception

import com.threerings.s3.client.S3ObjectListing
import com.threerings.s3.client.S3ObjectEntry

import com.threerings.s3.client.S3Object
import com.threerings.s3.client.acl.AccessControlList.StandardPolicy

import scala.collection.jcl.Conversions._

/**
 * Represents a remote S3 bucket.
 *
 * @param name Bucket name.
 * @param account S3 account.
 */
class Bucket (val name:String, account:S3Account) {
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

    /**
   * Fetch an S3 object with the provided key.
   *
   * @param key Object key.
   */
  def get (key:String): S3Object = {
    account.retry(conn.getObject(name, key))
  }

  /**
   * Upload an S3 object using a StandardPolicy.PRIVATE access
   * policy.
   *
   * @param obj Object to upload. Note that the final key name
   * will be relative to any path prefix used by this storage item.
   */
  def put (obj:S3Object): Unit = {
    this.put(obj, StandardPolicy.PRIVATE)
  }

  /**
   * Upload an S3 object
   *
   * @param obj Object to upload. Note that the final key name
   * will be relative to any path prefix used by this storage item.
   * @param policy Access policy to use for the uploaded item.
   */
  def put (obj:S3Object, policy:StandardPolicy): Unit = {
    account.retry(conn.putObject(name, obj, policy))
  }

  /**
   * Delete a remote S3 Object.
   * @param key S3 object key.
   */
  def delete (key:String) = {
    account.retry(conn.deleteObject(name, key))
  }

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
