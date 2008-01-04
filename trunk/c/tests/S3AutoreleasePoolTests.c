/*
 * S3AutoreleasePoolTests.c vi:ts=4:sw=4:expandtab:
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

/* Pool alloc/dealloc */
START_TEST (test_add) {
    S3AutoreleasePool *pool;
    S3String *obj;
    int i;

    /* Allocate a pool */
    pool = s3autorelease_pool_new();
    fail_if(pool == NULL, "s3autorelease_pool_new() returned NULL.\n");

    /* Allocate an object to auto-release */
    obj = s3string_new("Hello, World");

    /* Exercise the pool */
    for (i = 0; i < 4096; i++) {
        s3_retain(obj);
        s3autorelease_pool_add(pool, obj);
    }

    /* Release it */
    s3_release(pool);

    fail_unless(s3_reference_count(obj) == 1, "S3AutoreleasePool failed to release %d objects.", s3_reference_count(obj) - 1);
    s3_release(obj);
}
END_TEST

Suite *S3AutoreleasePool_suite(void) {
    Suite *s = suite_create("S3AutoreleasePool");

    TCase *tc_general = tcase_create("General");
    suite_add_tcase(s, tc_general);
    tcase_add_test(tc_general, test_add);

    return s;
}
