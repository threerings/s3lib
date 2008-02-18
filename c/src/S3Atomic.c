/*
 * S3Atomic.c vi:ts=4:sw=4:expandtab:
 * Amazon S3 Library
 *
 * Author: Landon Fuller <landonf@threerings.net>
 *
 * Copyright (c) 2005 - 2008 Landon Fuller <landonf@bikemonkey.org>
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
 * 3. Neither the name of Landon Fuller nor the names of any contributors
 *    may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
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

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include "S3Lib.h"

#include <pthread.h>


/**
 * @file
 * @brief Atomic Integer Operations
 * @author Landon Fuller <landonf@bikemonkey.org>
 */


/**
 * @defgroup S3Atomic Atomic Integer Operations
 * @ingroup S3Library
 * @internal
 * @{
 */

/** @internal Single mutex for all atomic operations. */
pthread_mutex_t atomic_mutex = PTHREAD_MUTEX_INITIALIZER;

/**
 * @internal
 * Perform an atomic increment of val by one, and return the new value.
 */
S3_PRIVATE uint32_t s3_slow_atomic_uint32_incr (volatile uint32_t *val) {
    uint32_t ret;

    pthread_mutex_lock(&atomic_mutex);
    ret = ++(*val);
    pthread_mutex_unlock(&atomic_mutex);

    return ret;
}

/**
 * @internal
 * Perform an atomic decrement of val by one, and return the new value.
 */
S3_PRIVATE uint32_t s3_slow_atomic_uint32_decr (volatile uint32_t *val) {
    uint32_t ret;

    pthread_mutex_lock(&atomic_mutex);
    ret = --(*val);
    pthread_mutex_unlock(&atomic_mutex);

    return ret;
}

/**
 * @internal
 * Atomically retrieve the value of the given integer.
 */
S3_PRIVATE uint32_t s3_slow_atomic_uint32_get (volatile uint32_t *val) {
    uint32_t ret;

    pthread_mutex_lock(&atomic_mutex);
    ret = *val;
    pthread_mutex_unlock(&atomic_mutex);

    return ret;
}

/*!
 * @} S3Atomic
 */