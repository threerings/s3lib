/*
 * Author: Landon Fuller <landonf@plausiblelabs.com>
 *
 * Copyright (c) 2009 Plausible Labs Cooperative, Inc.
 * All rights reserved.
 */

package com.plausiblelabs.s3.client

import com.threerings.s3.client.S3Connection

/**
 * Amazon S3 Account.
 *
 * @param awsId AWS non-secret account identifier.
 * @param awsKey AWS secret key.
 */
class S3Account (awsId:String, awsKey:String) {
  /** HTTPS connection */
  private[s3] val conn = new S3Connection(awsId, awsKey)

  /**
   * Return a reference to the named S3 bucket.
   *
   * @param name S3 bucket name
   */
  def bucket (name:String) = new Bucket(name, this)

  /**
   * Provides automatic retry if a transient S3 server exception occurs.
   *
   * @param expression Expression to execute
   */
  private[client] def retry[T] (expression: => T): T = {
    /* Retry up to 30 times */
    retry(30)(expression)
  }

  /**
   * Provides automatic retry if a transient S3 server exception occurs.
   *
   * @param count Number of times to retry
   * @param expression Expression to execute
   */
  private[client] def retry[T] (count:Int) (expression: => T): T = {
    import com.threerings.s3.client.S3Exception

    if (count <= 0)
      expression
    else try {
      expression
    } catch {
      /* Retry for transient errors */
      case e:S3Exception if (e.isTransient() == true) =>
        retry (count - 1) (expression)
    }
  }
}
