/* 
 * StreamUtil.java vi:ts=4:sw=4:expandtab:
 *
 * Copyright (c) 2007 Three Rings Design, Inc.
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

package com.threerings.s3.pipe;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;

/*
 * Re-usable stream-related utilities.
 *
 * Stream key names are heirarchical, using "." as a delimiter, in order to leverage S3's
 * support for condensing common prefixes and dropping suffixes when listing objects.
 * This allows for the creation of per-stream "structures". Example:
 *  stream.info
 *  stream.block.0
 *  stream.block.1
 *
 * We base64-encode the stream name to ensure that it does not contain any "." delimiters.
 */
class StreamUtil {
    
    protected StreamUtil (String streamName) {

        /* Save the unencoded stream name. */
        _streamName = streamName;

        /* Cache the encoded stream name. */
        try {
            Base64 encoder = new Base64();
            byte[] data = _streamName.getBytes(NAME_ENCODING);
            _encodedStreamName = new String(encoder.encode(data), "ascii");            
        } catch (UnsupportedEncodingException uee) {
            // utf-8 and ascii must always be available.
            throw new RuntimeException("Missing a standard encoding", uee);
        }
    }

    /**
     * Return the S3 key for a given encoded stream name's info field.
     */
    public String streamInfoKey () {
        return _encodedStreamName + FIELD_DELIMETER + INFO_FIELD;
    }


    /**
     * Return the S3 key for a given encoded stream name and block id.
     */
    public String streamBlockKey (long blockId) {
        return _encodedStreamName + FIELD_DELIMETER + BLOCK_FIELD +
            FIELD_DELIMETER + Long.toString(blockId);
    }

    /** Stream name. */
    private String _streamName;

    /** Stream encoded name. */
    private String _encodedStreamName;

    /** Field delimiter. */
    private static final String FIELD_DELIMETER = ".";

    /** Block data field. */
    private static final String BLOCK_FIELD = "block";

    /** Info data field. */
    private static final String INFO_FIELD = "info";

    /** Character set encoding used for base64'd stream names. */
    private static final String NAME_ENCODING = "utf-8";
}
