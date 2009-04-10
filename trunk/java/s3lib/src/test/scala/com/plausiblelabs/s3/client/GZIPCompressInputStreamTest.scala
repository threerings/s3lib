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

import java.io.{ByteArrayInputStream, BufferedOutputStream, BufferedInputStream, InputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

class GZIPCompressInputStreamTest {
  /**
   * Read the given input stream in 4 byte chunks, writing the entirity
   * to the given byte array.
   */
  final private def read (input:InputStream, array: Array[Byte], total:Int): Int= {
    val count = input.read(array, total, 4.min(array.length - total))
    if (count <= 0)
      total
    else
      read(input, array, total+count)
  }



  @Test def testFilter = {
    /* Initialize an 8k source input stream */
    val bytes = new Array[Byte](8192)
    for (i <- 0.until(8192))
      bytes(i) = i.toByte

    val source = new BufferedInputStream(new ByteArrayInputStream(bytes))

    /* Create the filter. The input data is read, gzip'd, ungzip'd */
    val inputBytes = new Array[Byte](bytes.length)
    val filter = new GZIPInputStream(new GZIPCompressInputStream(source))

    /* Read the whole set */
    read (filter, inputBytes, 0)
    
    /* Verify successful read */
    assertTrue(bytes.deepEquals(inputBytes))
  }
}
