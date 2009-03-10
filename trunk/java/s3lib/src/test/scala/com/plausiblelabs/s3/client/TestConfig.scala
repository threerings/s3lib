/*
 * Author: Landon Fuller <landonf@plausiblelabs.com>
 *
 * Copyright (c) 2009 Plausible Labs Cooperative, Inc.
 * All rights reserved.
 */

package com.plausiblelabs.s3.client

import com.threerings.s3.client.S3TestConfig

/**
 * Generic test configuration.
 *
 * Wraps the legacy java S3TestConfig.
 */
trait TestConfig {
  /** Return the testing AWS ID */
  def awsId = S3TestConfig.getId

  /** Return the testing AWS key */
  def awsKey = S3TestConfig.getKey
}
