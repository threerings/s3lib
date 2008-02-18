/*
 * S3AtomicTests.c vi:ts=4:sw=4:expandtab:
 * Amazon S3 Library Unit Tests
 *
 * Author: Landon Fuller <landonf@threerings.net>
 *
 * Copyright (c) 2006 - 2007 Landon Fuller <landonf@bikemonkey.org>
 * Copyright (c) 2006 - 2007 Three Rings Design, Inc.
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

#include "tests.h"

START_TEST (test_increment) {
    uint32_t test;
    
    test = 4;
    fail_unless(s3_atomic_uint32_incr(&test) == 5);
    fail_unless(test == 5);
    
    test = INT32_MAX;
    fail_unless(s3_atomic_uint32_incr(&test) == ((uint32_t) INT32_MAX) + 1);
    fail_unless(test == ((uint32_t) INT32_MAX) + 1);
    
    test = UINT32_MAX - 1;
    fail_unless(s3_atomic_uint32_incr(&test) == UINT32_MAX);
    fail_unless(test == UINT32_MAX);
    
    test = UINT32_MAX;
    fail_unless(s3_atomic_uint32_incr(&test) == 0);
    fail_unless(test == 0);
}
END_TEST

START_TEST (test_decrement) {
    uint32_t test;

    test = 4;
    fail_unless(s3_atomic_uint32_decr(&test) == 3);
    fail_unless(test == 3);
    
    test = 0;
    fail_unless(s3_atomic_uint32_decr(&test) == UINT32_MAX);
    fail_unless(test == UINT32_MAX);
}
END_TEST

Suite *S3Atomic_suite(void) {
    Suite *s = suite_create("S3Atomic");

    TCase *tc_general = tcase_create("General");
    suite_add_tcase(s, tc_general);
    tcase_add_test(tc_general, test_increment);
    tcase_add_test(tc_general, test_decrement);

    return s;
}
