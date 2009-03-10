/*
 * Author: Landon Fuller <landonf@plausiblelabs.com>
 *
 * Copyright (c) 2009 Plausible Labs Cooperative, Inc.
 * All rights reserved.
 */

package com.plausiblelabs.s3.client

import org.junit._
import org.junit.Assert._

import java.util.UUID

import com.threerings.s3.client.S3ByteArrayObject
import com.threerings.s3.client.acl.AccessControlList.StandardPolicy

class BucketTest extends TestConfig {
  val conn = new S3Account(awsId, awsKey)
  var bucket:Bucket = _

  @Before
  def setUp:Unit = {
    /* Create the per-test bucket */
    bucket = conn.bucket(UUID.randomUUID.toString)
    bucket.create
  }

  @After
  def tearDown:Unit = {
    for (obj <- bucket.objects) {
      bucket.delete(obj.getKey)
    }

    bucket.delete
  }
  
  /** Test PUT support */
  @Test def testPut = {
    bucket.put(new S3ByteArrayObject("obj", Array()))
    assertEquals("obj", bucket.objects.first.getKey)
  }
  
  @Test def testGet = {
    /* Upload an object containing a single '5' byte */
    bucket.put(new S3ByteArrayObject("obj", Array(5)))
    
    /* Fetch the object and read its data */
    val obj = bucket.get("obj")
    val bytes = new Array[Byte](1)
    obj.getInputStream.read(bytes)
    
    /* Verify the byte array matches */
    assertEquals(1, bytes.length)
    assertEquals(5.toByte, bytes.first)
  }
  
  @Test def testDelete = {
    /* Upload one object and verify that the bucket is populated */
    bucket.put(new S3ByteArrayObject("obj", Array(1)))
    assertEquals(1, bucket.objects.toList.length)
    
    /* Delete the object and verify that the bucket is now empty */
    bucket.delete("obj")
    assertEquals(0, bucket.objects.toList.length)
  }

  /** Test list bucket support */
  @Test def testList = {
    /* Upload five string objects */
    for (i <- 1 to 5) {
      val obj = new S3ByteArrayObject("obj_" + i, Array(42))
      bucket.put(obj, StandardPolicy.PRIVATE)
    }

    /* Fetch the objects using the internal listing method to 
     * ensure that 5 different requests are issued */
    val objs = bucket.objects(null, null, null, 1).toList

    /* Verify that all five objects were returned */
    assertEquals(5, objs.length)
  }
}
