/*
 * Author: Landon Fuller <landonf@plausiblelabs.com>
 *
 * Copyright (c) 2009 Plausible Labs Cooperative, Inc.
 * All rights reserved.
 */

package com.plausiblelabs.s3.client

import org.junit._
import org.junit.Assert._

class S3AccountTest {
  @Test
  def testBucket {
    val bucket = new S3Account("key", "id").bucket("bucket")
    assertEquals("bucket", bucket.name)
  }
}
