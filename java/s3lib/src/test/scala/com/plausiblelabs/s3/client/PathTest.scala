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
    path.put("obj", S3Object.bytes(Array(5)))

    val entry = bucket.objects.first
    assertEquals(entry.getKey, "/test/obj")
  }

  @Test def testGet {
    bucket.put("/test/obj", S3Object.bytes(Array(5)))
    val obj = path.get("obj")
    assertNotNull(obj)
  }
}
