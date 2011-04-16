/*
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

package com.threerings.s3.client;

/**
 * Represents a media type, including the mime type and content encoding.
 */
public class MediaType {
    /**
     * Construct a new media type
     *
     * @param mimeType IANA media (MIME) type.
     */
    public MediaType (String mimeType) {
        this(mimeType, null);
    }

    /**
     * Construct a new media type
     *
     * @param mimeType IANA media (MIME) type.
     * @param contentEncoding An IANA registered content coding. Specifies the encoding/decoding mechanism
     * that must be applied in order to obtain data corresponding to the provided mimeType.
     */
    public MediaType (String mimeType, String contentEncoding) {
        _mimeType = mimeType;
        _contentEncoding = contentEncoding;
    }

    /**
     * Return the MIME type
     */
    public String getMimeType () {
      return _mimeType;
    }

    /**
     * Return the content encoding, if any.
     * May be null
     */
    public String getContentEncoding () {
      return _contentEncoding;
    }

    @Override
    public boolean equals (Object obj) {
        /* Short-cut identity check */
        if (this == obj)
            return true;

        /* Check for null, verify same class */
        if (obj == null || obj.getClass() != this.getClass())
            return false;

        /* Safe to cast */
        MediaType other = (MediaType) obj;

        /* Check the mime type */
        if (!_mimeType.equals(other.getMimeType()))
            return false;

        /* Check the content encoding. May be null */
        if (_contentEncoding == null) {
            if (other.getContentEncoding() != null)
              return false;
        } else {
            if (!_contentEncoding.equals(other.getContentEncoding()))
              return false;
        }

        /* Is equal */
        return true;
    }

    @Override
    public int hashCode() {
      int hash = 5;
      hash = 31 * hash + _mimeType.hashCode();
      hash = 31 * hash + (_contentEncoding != null ? _contentEncoding.hashCode() : 0);
      return hash;
    }

    /** IANA mime type */
    private final String _mimeType;

    /** IANA content coding */
    private final String _contentEncoding;
}
