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

class PathTest extends TestConfig {
  val conn = new S3Account(awsId, awsKey)
  var bucket:Bucket = _
  var path:Path = _

  @Before
  def setUp:Unit = {
    /* Create the per-test bucket */
    bucket = conn.bucket(UUID.randomUUID.toString)
    bucket.create
    path = new Path(bucket, "/test//")
  }

  @After
  def tearDown:Unit = {
    /* Drop the per-test bucket */
    for (obj <- bucket.objects)
      bucket.delete(obj.getKey)
    bucket.delete
  }

  @Test def testPut {
    path.put(new S3ByteArrayObject("obj", Array(5)))

    val entry = bucket.objects.first
    assertEquals(entry.getKey, "/test/obj")
  }

  @Test def testGet {
    bucket.put(new S3ByteArrayObject("/test/obj", Array(5)))
    val obj = path.get("obj")
    assertEquals("/test/obj", obj.getKey)
  }
}
