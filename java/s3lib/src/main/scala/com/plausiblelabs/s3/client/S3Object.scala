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

import com.threerings.s3.client.{S3Object => JavaS3Object, S3ClientException}

import java.io.InputStream
import java.io.File

import java.util.Date
import java.io.{ByteArrayInputStream, BufferedInputStream, FileInputStream}
import java.util.zip.GZIPInputStream

import java.security.MessageDigest

/**
 * Companion object
 */
object S3Object {
  /** Default media type. Used if no media type is specified. */
  private val DEFAULT_MEDIA_TYPE = new MediaType("binary/octet-stream")

  /** IANA-registered content coding for GZIP compression */
  private val GZIP_CONTENT_CODING = "gzip"

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
   * @param mediaType File media type
   */
  def file (file:File, mediaType:MediaType): S3Object = {
    val md5 = Some(md5input(new FileInputStream(file)))
    val lastModified = file.lastModified match {
      case 0 => None
      case n:Long => Some(new Date(n))
    }

    new S3SimpleObject(mediaType, lastModified, new FileInputStream(file), md5, Some(file.length))
  }

  /**
   * Create a new file-backed S3Object instance.
   *
   * @param file File containing the object data.
   */
  def file (file:File): S3Object = this.file(file, DEFAULT_MEDIA_TYPE)


  /**
   * Create a new byte array backed S3Object instance.
   *
   * @param bytes Bytes to use as file data.
   * @param MediaType Object media type
   */
  def bytes (bytes:Array[Byte], mediaType:MediaType): S3Object = {
     val md5 = Some(md5input(new ByteArrayInputStream(bytes)))
     new S3SimpleObject(mediaType, None, new ByteArrayInputStream(bytes), md5, Some(bytes.length))
  }

  /**
   * Create a new byte array backed S3Object instance.
   *
   * @param bytes Bytes to use as file data.
   * @param mimeType Object mime type
   */
  def bytes (bytes:Array[Byte]): S3Object = this.bytes(bytes, DEFAULT_MEDIA_TYPE)


  /**
   * Content coding filters automatically handle wrapping an S3Object and performing
   * data encoding/decoding.
   */
  object filter {
    /**
     * Apply a GZIP content encoding to the provided S3Object, returning a new wrapping
     * object that will automatically gzip the contents. If a content coding has already been set,
     * this method will throw an exception.
     *
     * @throws S3ClientException Thrown if a cotent coding has already been supplied
     * for the given object.
     */
    def gzip (obj:S3Object): S3Object = {
      /* Verify that no coding is already set */
      if (obj.mediaType.contentEncoding.isDefined)
        throw new S3ClientException("Content encoding has already been set for this object")

      /* Create the new media type and wrapping S3Object */
      val mtype = new MediaType(obj.mediaType.mimeType, Some(GZIP_CONTENT_CODING))
      new S3SimpleObject(mtype, obj.lastModified, new GZIPCompressInputStream(new BufferedInputStream(obj.inputStream)), None, None)
    }

    /**
     * Wrap the provided object, automatically decompressing the input data. If the coding is
     * not set, or is set incorrectly, no decoding will be performed.
     *
     * @param obj Object to wrap.
     */
    def gunzip (obj:S3Object): S3Object = {
      /* If no coding was supplied return the unmodified object */
      if (obj.mediaType.contentEncoding.isEmpty || !obj.mediaType.contentEncoding.get.equals(GZIP_CONTENT_CODING))
        return obj

      /* Create the new media type and wrapping S3Object */
      val mtype = new MediaType(obj.mediaType.mimeType, None)
      new S3SimpleObject(mtype, obj.lastModified, new GZIPInputStream(obj.inputStream), None, None)
    }
  }
}


/** Default concrete S3Object implementation */
private[client] class S3SimpleObject (val mediaType:MediaType,
                                      val lastModified:Option[Date],
                                      val inputStream:InputStream,
                                      val md5:Option[Array[Byte]],
                                      val length:Option[Long]) extends S3Object;

/**
 * Represents a local or remote S3 object, and its associated metadata.
 */
trait S3Object {
  /** The object's media type. */
  def mediaType:MediaType

  /** Last modified date. */
  def lastModified:Option[Date]

  /** Input stream, used to read the object's data. */
  def inputStream:InputStream

  /** Object's MD5 checksum */
  def md5:Option[Array[Byte]]

  /** Length in bytes */
  def length:Option[Long]
}
