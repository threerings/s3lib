/*
 * S3String.c vi:ts=4:sw=4:expandtab:
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

#include <string.h>

#include "tests.h"

/* String alloc/dealloc */
START_TEST (test_new) {
    S3String *string = s3string_new("hello");
    fail_unless(strcmp("hello", s3string_cstring(string)) == 0);
    s3_release(string);
}
END_TEST

START_TEST (test_copy) {
    S3String *string = s3string_new("hello");
    S3String *copy = s3string_copy(string);

    fail_unless(strcmp("hello", s3string_cstring(copy)) == 0);

    s3_release(copy);
    s3_release(string);
}
END_TEST

START_TEST (test_withformat) {
    S3String *string = s3string_withformat("%s %s", "test", "string");
    fail_unless(s3_equals(string, S3STR("test string")));
}
END_TEST

START_TEST (test_startswith) {
    S3String *string = s3string_new("hello");
    fail_unless(s3string_startswith(string, S3STR("he")));
    fail_unless(s3string_startswith(string, S3STR("hel")));
    fail_if(s3string_startswith(string, S3STR("fhel")));
    s3_release(string);
}
END_TEST

START_TEST (test_lowercase) {
    S3String *string = s3string_new("HeLlO WoRlD");
    S3String *lower = s3string_lowercase(string);
    fail_unless(s3_equals(lower, S3STR("hello world")));
    s3_release(string);
}
END_TEST

START_TEST (test_cstring) {
    S3String *string = s3string_new("hello");
    fail_unless(strcmp("hello", s3string_cstring(string)) == 0);
    s3_release(string);
}
END_TEST

START_TEST (test_length) {
    S3String *string = s3string_new("hello");
    fail_unless(s3string_length(string) == strlen(s3string_cstring(string)));
    s3_release(string);
}
END_TEST

START_TEST (test_hash) {
    S3String *hello = S3STR("hello");
    S3String *helloagain = S3STR("hello");
    S3String *bye = S3STR("bye");

    /* Two different strings should have different hash values */
    fail_if(s3_hash(hello) == s3_hash(bye));

    /* The hash value for the same string value should always be the same */
    fail_unless(s3_hash(hello) == s3_hash(helloagain));
}
END_TEST

START_TEST (test_cstring_hash) {
    const char *hello = "hello";
    const char *helloagain = "hello";
    const char *bye = "bye";

    /* Two different strings should have different hash values */
    fail_if(s3cstring_hash(hello) == s3cstring_hash(bye));

    /* The hash value for the same string value should always be the same */
    fail_unless(s3cstring_hash(hello) == s3cstring_hash(helloagain));
}
END_TEST

START_TEST (test_equals) {
    S3String *hello = s3string_new("hello");
    S3String *helloagain = s3string_new("hello");
    S3String *bye = s3string_new("bye");

    /* Two different strings should not be equal */
    fail_if(s3_equals(hello, bye));

    /* The same string value should always be equal */
    fail_unless(s3_equals(hello, helloagain));

    s3_release(hello);
    s3_release(helloagain);
    s3_release(bye);
}
END_TEST

Suite *S3String_suite(void) {
    Suite *s = suite_create("S3String");

    TCase *tc_general = tcase_create("General");
    suite_add_tcase(s, tc_general);
    tcase_add_test(tc_general, test_new);
    tcase_add_test(tc_general, test_copy);
    tcase_add_test(tc_general, test_withformat);
    tcase_add_test(tc_general, test_startswith);
    tcase_add_test(tc_general, test_lowercase);
    tcase_add_test(tc_general, test_cstring);
    tcase_add_test(tc_general, test_length);
    tcase_add_test(tc_general, test_hash);
    tcase_add_test(tc_general, test_cstring_hash);
    tcase_add_test(tc_general, test_equals);

    return s;
}
