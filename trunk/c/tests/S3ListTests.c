/*
 * S3List.c vi:ts=4:sw=4:expandtab:
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

/* List alloc/dealloc */
START_TEST (test_new) {
    S3List *list = s3list_new();
    s3_release(list);
}
END_TEST

/* Append an element */
START_TEST (test_append) {
    S3List *list = s3list_new();
    S3ListIterator *i;
    S3String *string = s3string_new("hello");

    fail_unless(s3list_append(list, string));
    i = s3list_iterator_new(list);
    fail_unless(strcmp(s3string_cstring(s3list_iterator_next(i)), "hello") == 0);

    s3_release(i);
    s3_release(string);
    s3_release(list);
}
END_TEST

/* Clone a list. */
START_TEST (test_copy) {
    S3List *orig;
    S3List *copy;
    S3ListIterator *i;
    S3String *hello = s3string_new("hello");
    S3String *world = s3string_new("world");

    orig = s3list_new();
    s3list_append(orig, hello);
    s3list_append(orig, world);

    copy = s3list_copy(orig);    
    i = s3list_iterator_new(copy);

    /* Check the first node */
    fail_unless(strcmp(s3string_cstring(s3list_iterator_next(i)), "hello") == 0);

    /* Check the second */
    fail_unless(strcmp(s3string_cstring(s3list_iterator_next(i)), "world") == 0);

    s3_release(i);
    s3_release(hello);
    s3_release(world);
    s3_release(orig);
    s3_release(copy);
}
END_TEST

/* Test list iteration */
START_TEST (test_next) {
    S3List *list = s3list_new();
    S3ListIterator *i;
    S3String *hello = s3string_new("hello");
    S3String *world = s3string_new("world");

    /* Append two elements */
    fail_unless(s3list_append(list, hello));
    fail_unless(s3list_append(list, world));

    /* Fetch the two elements */
    i = s3list_iterator_new(list);
    fail_unless(strcmp(s3string_cstring(s3list_iterator_next(i)), "hello") == 0);
    fail_unless(strcmp(s3string_cstring(s3list_iterator_next(i)), "world") == 0);

    /* Should hit the end of the list, and keep returning NULL */
    fail_unless(s3list_iterator_next(i) == NULL);
    fail_unless(s3list_iterator_next(i) == NULL);

    s3_release(i);
    s3_release(list);
    s3_release(hello);
    s3_release(world);
}
END_TEST

Suite *S3List_suite(void) {
    Suite *s = suite_create("S3List");

    TCase *tc_general = tcase_create("General");
    suite_add_tcase(s, tc_general);
    tcase_add_test(tc_general, test_new);
    tcase_add_test(tc_general, test_append);
    tcase_add_test(tc_general, test_copy);
    tcase_add_test(tc_general, test_next);

    return s;
}
