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
