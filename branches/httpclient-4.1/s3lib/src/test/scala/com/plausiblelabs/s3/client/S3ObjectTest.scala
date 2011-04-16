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

import java.io.File
import java.io.FileOutputStream

import org.apache.commons.io.IOUtils

import org.apache.commons.codec.binary.Hex;

/**
 * Test handling of file-based S3Object construction.
 */
class S3ObjectTest {
  /** Test data */
  val TEST_DATA = "Hello, World!".getBytes("utf8");

  /** Pre-computed MD5 Checksum for test data. */
  val TEST_DATA_MD5 = "65a8e27d8879283831b664bd8b7f0ad4";

  /** Execute the provided function, passing it a newly
   * created temporary file. The file will be deleted
   * on function exit */
  private def tempFile (f:(File) => Unit) = {
    val file = File.createTempFile("S3ObjectTest", null)
    try {
      f(file)
    } finally {
      file.delete
    }
  }

  /** Convert the provided bytes to the hexidecimal string representation */
  private def hex (bytes:Array[Byte]) = new String(Hex.encodeHex(bytes));

  /** Test handling of file objects */
  @Test
  def testFile = {
    tempFile {file =>
      new FileOutputStream(file).write(TEST_DATA);
      val obj = S3Object.file(file)

      assertEquals(TEST_DATA.length.toLong, obj.length.get)
      assertEquals(TEST_DATA_MD5, hex(obj.md5.get))
    }
  }

  /** Test byte arrays */
  @Test
  def testBytes = {
    val obj = S3Object.bytes(TEST_DATA)
    assertEquals(TEST_DATA.length.toLong, obj.length.get)
    assertEquals(TEST_DATA_MD5, hex(obj.md5.get))
  }

  /** Test GZIP conversion */
  @Test
  def testGZIP = {
    /* Create a GZIP'd object, and a GUNZIPing wrapper */
    val obj = S3Object.filter.gzip(S3Object.bytes(TEST_DATA))
    val gunzip = S3Object.filter.gunzip(obj)

    /* Test the content encoding settings */
    assertEquals("gzip", obj.mediaType.contentEncoding.get)
    assertTrue(gunzip.mediaType.contentEncoding.isEmpty)

    /* Verify the data conversion */
    val extracted = IOUtils.toByteArray(gunzip.inputStream)
    assertTrue(extracted.deepEquals(TEST_DATA))
  }
}