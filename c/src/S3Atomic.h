/*
 * S3Account.h vi:ts=4:sw=4:expandtab:
 * Amazon S3 Library
 *
 * Author: Landon Fuller <landonf@threerings.net>
 *
 * Copyright (c) 2008 Landon Fuller <landonf@bikemonkey.org>
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

#ifndef S3ATOMIC_H
#define S3ATOMIC_H

/**
 * @file
 * @brief Atomic Integer Operations
 * @author Landon Fuller <landonf@bikemonkey.org>
 */

/*!
 * @addtogroup S3Atomic
 * @{
 */

#ifdef __i386__

/**
 * Atomically increment a 32-bit integer and return the new value.
 */
static inline uint32_t s3_atomic_uint32_incr (volatile uint32_t *val) {
    uint32_t tmp;

    asm volatile (
        "   lock; xaddl     %0, %1;"
        : "=r" (tmp), "=m" (*val)
        : "0" (1), "m" (*val)
    );

    return tmp + 1;
}

/**
 * Atomically decrement a 32-bit integer and return the new value.
 */
static inline uint32_t s3_atomic_uint32_decr (volatile uint32_t *val) {
    uint32_t tmp;

    asm volatile (
        "   lock; xaddl     %0, %1;"
        : "=r" (tmp), "=m" (*val)
        : "0" (-1), "m" (*val)
    );

    return tmp - 1;
}

#endif


/*!
 * @} S3Atomic
 */

#endif /* S3ATOMIC_H */
