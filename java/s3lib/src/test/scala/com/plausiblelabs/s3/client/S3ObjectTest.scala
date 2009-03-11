/*
 * Author: Landon Fuller <landonf@plausiblelabs.com>
 *
 * Copyright (c) 2009 Plausible Labs Cooperative, Inc.
 * All rights reserved.
 */

package com.plausiblelabs.s3.client

import org.junit._
import org.junit.Assert._

import java.io.File
import java.io.FileOutputStream

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

      assertEquals(TEST_DATA.length.toLong, obj.length)
      assertEquals(TEST_DATA_MD5, hex(obj.md5.get))
    }
  }

  /** Test byte arrays */
  @Test
  def testBytes = {
    val obj = S3Object.bytes(TEST_DATA)
    assertEquals(TEST_DATA.length.toLong, obj.length)
    assertEquals(TEST_DATA_MD5, hex(obj.md5.get))
  }
}