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

import com.threerings.s3.client.{S3Object => JavaS3Object}

import java.io.InputStream
import java.io.File

import java.util.Date
import java.io.FileInputStream
import java.io.ByteArrayInputStream

import java.security.MessageDigest

/**
 * Companion object
 */
object S3Object {
  /** Default mime-type. Used if no mime-type is specified. */
  private val DEFAULT_MIME_TYPE = "binary/octet-stream"

  /** Compute and return the MD5 value for the given input stream */
  private def md5input (input:InputStream): Array[Byte] = {
         // Initialize
         val md = MessageDigest.getInstance("md5")
         val data = new Array[Byte](1024);

         // Compute the digest
         def compute (nbytes:Int) {
           if (nbytes > 0) {
             md.update(data, 0, nbytes)
             compute(input.read(data))
           }
         }
         compute(input.read(data))

         return md.digest();
  }

  /**
   * Create a new file-backed S3Object instance.
   *
   * @param file File containing the object data.
   * @param mimeType File mime type
   */
  def file (file:File, mimeType:String): S3Object = {
    val md5 = Some(md5input(new FileInputStream(file)))
    val lastModified = file.lastModified match {
      case 0 => None
      case n:Long => Some(new Date(n))
    }

    new S3SimpleObject(mimeType, lastModified, new FileInputStream(file), md5, file.length)
  }

  /**
   * Create a new file-backed S3Object instance.
   *
   * @param file File containing the object data.
   */
  def file (file:File): S3Object = this.file(file, DEFAULT_MIME_TYPE)


  /**
   * Create a new byte array backed S3Object instance.
   *
   * @param bytes Bytes to use as file data.
   * @param mimeType Object mime type
   */
  def bytes (bytes:Array[Byte], mimeType:String): S3Object = {
     val md5 = Some(md5input(new ByteArrayInputStream(bytes)))
     new S3SimpleObject(mimeType, None, new ByteArrayInputStream(bytes), md5, bytes.length)
  }

  /**
   * Create a new byte array backed S3Object instance.
   *
   * @param bytes Bytes to use as file data.
   * @param mimeType Object mime type
   */
  def bytes (bytes:Array[Byte]): S3Object = this.bytes(bytes, DEFAULT_MIME_TYPE)
}


/** Default concrete S3Object implementation */
private[client] class S3SimpleObject (val mimeType:String,
                                      val lastModified:Option[Date],
                                      val inputStream:InputStream,
                                      val md5:Option[Array[Byte]],
                                      val length:Long) extends S3Object;

/**
 * Represents a local or remote S3 object, and its associated metadata.
 */
trait S3Object {
  /** The object's mime type. */
  def mimeType:String

  /** Last modified date. */
  def lastModified:Option[Date]

  /** Input stream, used to read the object's data. */
  def inputStream:InputStream

  /** Object's MD5 checksum */
  def md5:Option[Array[Byte]]

  /** Length in bytes. */
  def length:Long
}
