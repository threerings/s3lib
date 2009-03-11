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

import org.junit._
import org.junit.Assert._

import java.util.UUID

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
    bucket.put("obj", S3Object.bytes(Array()))
    assertEquals("obj", bucket.objects.first.getKey)
  }
  
  @Test def testGet = {
    /* Upload an object containing a single '5' byte */
    bucket.put("obj", S3Object.bytes(Array(5)))
    
    /* Fetch the object and read its data */
    val obj = bucket.get("obj")
    val bytes = new Array[Byte](1)
    obj.inputStream.read(bytes)
    
    /* Verify the byte array matches */
    assertEquals(1, bytes.length)
    assertEquals(5.toByte, bytes.first)
  }
  
  @Test def testDelete = {
    /* Upload one object and verify that the bucket is populated */
    bucket.put("obj", S3Object.bytes(Array(1)))
    assertEquals(1, bucket.objects.toList.length)
    
    /* Delete the object and verify that the bucket is now empty */
    bucket.delete("obj")
    assertEquals(0, bucket.objects.toList.length)
  }

  /** Test list bucket support */
  @Test def testList = {
    /* Upload five string objects */
    for (i <- 1 to 5) {
      bucket.put("obj_" + i, S3Object.bytes(Array(42)), StandardPolicy.PRIVATE)
    }

    /* Fetch the objects using the internal listing method to 
     * ensure that 5 different requests are issued */
    val objs = bucket.objects(null, null, null, 1).toList

    /* Verify that all five objects were returned */
    assertEquals(5, objs.length)
  }
}
