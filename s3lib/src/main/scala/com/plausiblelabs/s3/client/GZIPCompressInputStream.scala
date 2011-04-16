/*
 * Author: Landon Fuller <landonf@plausiblelabs.com>
 *
 * Copyright (c) 2009 Plausible Labs Cooperative, Inc.
 * All rights reserved.
 */

package com.plausiblelabs.s3.client

import java.io.InputStream
import java.util.zip.{CheckedInputStream, CRC32, Deflater, DeflaterInputStream}

/**
 * Companion object
 */
private[client] object GZIPCompressInputStream {
  /** GZIP Header Magic */
  private object GZIP_MAGIC {
    val ID1 = 0x1f.toByte
    val ID2 = 0x8b.toByte
  }

  /** GZIP header as defined by RFC 1952 */
  private val HEADER:Array[Byte] = Array(
    GZIP_MAGIC.ID1,      // ID (IDentification 1)
    GZIP_MAGIC.ID2,      // ID (IDentification 2)
    Deflater.DEFLATED,   // CM (Compression Method)
    0,                   // FLG (FLaGs)
    0,                   // MTIME (Modification TIME) uint32-0
    0,                   // MTIME (Modification TIME) uint32-1
    0,                   // MTIME (Modification TIME) uint32-2
    0,                   // MTIME (Modification TIME) uint32-3
    0,                   // XFL (eXtra FLags)
    0                    // OS (Operating System)
  )

  /** GZIP Trailer length */
  private val TRAILER_LENGTH = 8
}


/**
 * Implements a stream filter for compressing input data using the GZIP format.
 *
 * @param source The input stream containing the source data.
 * @Param crc An initialized CRC32 instance.
 */
private[client] class GZIPCompressInputStream (source:InputStream, crc:CRC32)
  extends DeflaterInputStream(new CheckedInputStream(source, crc), new Deflater(Deflater.DEFAULT_COMPRESSION, true))
{
  /**
   * Instantiate a new GZIP compression input filter.
   *
   * @param source Input stream to compress.
   */
  def this (source:InputStream) = this(source, new CRC32)

  /** Output buffer. Must be at least large enough for the GZIP header and trailer */
  private object buffer {
    /** Buffer position */
    var pos = 0

    /** Buffer length */
    var length = 0

    /** Buffer data */
    val data = {
      import GZIPCompressInputStream._

      /* Prime the buffer with the GZIP header */
      val b = new Array[Byte](HEADER.length.max(TRAILER_LENGTH))
      HEADER.copyToArray(b, 0)
      length = HEADER.length
      b
    }
  }

  /** If true, the GZIP trailer has been written */
  private var trailerWritten = false
  
  /**
   * Write the final GZIP trailer to the given buffer
   */
  private def writeTrailer (output:Array[Byte], pos:Int): Int = {
    /* Write the trailer */
    var count = writeInt(crc.getValue.toInt, output, pos)
    count += writeInt(this.`def`.getTotalIn, output, pos+count)
    count
  }

  /** Write a 32-bit little endian integer to the provided array */
  private def writeInt(value:Int, output:Array[Byte], pos:Int): Int = {
    output(pos+3) = ((value >>> 24) & 0xFF).toByte
    output(pos+2) = ((value >>> 16) & 0xFF).toByte
    output(pos+1) = ((value >>> 8) & 0xFF).toByte
    output(pos+0) = ((value >>> 0) & 0xFF).toByte

    /* 32-bit value, 4 bytes */
    return 4
  }

  override def read (bytes:Array[Byte], off:Int, len:Int): Int = {
    /* Check for internal buffer contents. This is used to provide
     * the header or trailer, and always takes precedence */
    if (buffer.length - buffer.pos > 0) {
      /* Write data from the internal buffer */
      val count = len.min(buffer.length - buffer.pos)
      Array.copy(buffer.data, buffer.pos, bytes, off, count)

      /* Advance the internal buffer position */
      buffer.pos += count

      return count
    }


    /* Attempt to read compressed input data */
    val count = super.read(bytes, off, len)
    if (count > 0) {
      return count
    }

    /* If the stream has reached completion, write out the GZIP trailer and re-attempt
     * the read */
    if (count <= 0 && !trailerWritten) {
      buffer.pos = 0
      buffer.length = writeTrailer(buffer.data, buffer.pos)
      trailerWritten = true
      return read(bytes, off, len)
    } else {
      return count
    }
  }
}
