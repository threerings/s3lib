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
    System.out.println("Running cleanup")
    for (obj <- bucket.objects) {
      System.out.println("Cleaning up " + obj)
      bucket.delete(obj.getKey)
    }

    bucket.delete
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
