/*
 * S3Lib.c vi:ts=4:sw=4:expandtab:
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

#include <stdlib.h>

#include "tests.h"

typedef struct S3Test {
    S3RuntimeBase base;
    uint8_t nothing;
} S3Test;

static void s3test_class_dealloc (S3TypeRef obj);

static S3RuntimeClass S3TestClass = {
    .version = S3_CURRENT_RUNTIME_CLASS_VERSION,
    .dealloc = &s3test_class_dealloc
};

static void s3test_class_dealloc (S3_UNUSED S3TypeRef obj) {
    /* Nothing to deallocate */
}

START_TEST (test_reference_counting) {
    S3Test *obj;

    obj = s3_object_alloc(&S3TestClass, sizeof(S3Test));

    fail_unless(s3_reference_count(obj) == 1);

    s3_retain(obj);
    fail_unless(s3_reference_count(obj) == 2);

    s3_release(obj);
    fail_unless(s3_reference_count(obj) == 1);

    s3_release(obj);
}
END_TEST

START_TEST (test_autorelease) {
    S3AutoreleasePool *pool = s3autorelease_pool_new();
    S3Test *obj;

    /* Create and auto release an object */
    obj = s3_object_alloc(&S3TestClass, sizeof(S3Test));
    s3_retain(obj);
    s3_autorelease(obj);

    /* Drop the pool and ensure that it released our object */
    s3_release(pool);
    fail_unless(s3_reference_count(obj) == 1);

    /* Clean up */
    s3_release(obj);
}
END_TEST

START_TEST (test_hash) {
    S3Test *obj;

    obj = s3_object_alloc(&S3TestClass, sizeof(S3Test));
    fail_unless(s3_hash(obj) == (long) obj);

    s3_release(obj);
}
END_TEST

START_TEST (test_equals) {
    S3Test *a;
    S3Test *b;

    a = s3_object_alloc(&S3TestClass, sizeof(S3Test));
    b = s3_object_alloc(&S3TestClass, sizeof(S3Test));
    fail_unless(s3_equals(a, a));
    fail_if(s3_equals(a, b));

    s3_release(a);
    s3_release(b);
}
END_TEST

START_TEST (test_instanceof) {
    S3Test *test = s3_object_alloc(&S3TestClass, sizeof(S3Test));
    S3String *string = s3string_new("test string");

    fail_unless(s3_instanceof(test, &S3TestClass));
    fail_if(s3_instanceof(string, &S3TestClass));

    s3_release(test);
    s3_release(string);
}
END_TEST

Suite *S3Lib_suite(void) {
    Suite *s = suite_create("S3Lib");

    TCase *tc_memory = tcase_create("Memory Management");
    suite_add_tcase(s, tc_memory);
    tcase_add_test(tc_memory, test_reference_counting);
    tcase_add_test(tc_memory, test_autorelease);

    TCase *tc_compare = tcase_create("Object Comparison");
    suite_add_tcase(s, tc_compare);
    tcase_add_test(tc_compare, test_hash);
    tcase_add_test(tc_compare, test_equals);
    tcase_add_test(tc_compare, test_instanceof);

    return s;
}
