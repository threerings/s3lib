/* 
 * S3RetryHandler.java vi:ts=4:sw=4:expandtab:
 *
 * Copyright (c) 2005 - 2007 Three Rings Design, Inc.
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

import com.threerings.s3.client.S3Exception;

/**
 * Retry on non-fatal S3 Exceptions.
 */
class S3RetryHandler {

    /**
     * Initialize the retry handler with the provided maximum retry count.
     */
    protected S3RetryHandler (int maxRetry) {
        _maxRetry = maxRetry;
    }

    /**
     * Returns true if the exception is transient and the caller
     * should retry. Otherwise, throws the provided exception.
     */
    protected boolean shouldRetry (S3Exception exception)
        throws S3Exception
    {
        /*
         * For now, all exceptions are blindly retried.
         * @todo Implement a transient flag for S3Exceptions to determine
         *  whether an operation should be retried.
         */
         _retryCount++;
         if (_retryCount > _maxRetry) {
             throw exception;
         } else {
             return true;
         }
    }

    /** Maximum number of retry attempts. */
    private int _maxRetry;

    /** Current number of attempts. */
    private int _retryCount = 0;
}
