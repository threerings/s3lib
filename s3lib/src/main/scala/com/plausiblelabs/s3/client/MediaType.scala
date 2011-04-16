/*
 * Author: Landon Fuller <landonf@plausiblelabs.com>
 *
 * Copyright (c) 2009 Plausible Labs Cooperative, Inc.
 * All rights reserved.
 */

package com.plausiblelabs.s3.client

/**
 * Represents a media type, including the mime type and content encoding.
 *
 * @param mimeType IANA media (MIME) type.
 * @param contentEncoding An IANA registered content coding. Specifies the encoding/decoding mechanism
 * that must be applied in order to obtain data corresponding to the provided mimeType.
 */
case class MediaType (mimeType:String, contentEncoding:Option[String]) {
  /** Create a new instance.
   * @param mimeType IANA media (MIME) type.
   */
  def this (mimeType:String) = this(mimeType, None)
}
