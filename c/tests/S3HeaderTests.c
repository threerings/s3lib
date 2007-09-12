/*
 * S3Request.c vi:ts=4:sw=4:expandtab:
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

START_TEST (test_header_dict_new) {
    S3HeaderDict *headers = s3header_dict_new();
    s3header_dict_free(headers);
}
END_TEST

START_TEST (test_header_dict_put) {
    S3HeaderDict *headers = s3header_dict_new();

    /* Put a value. */
    fail_unless(s3header_dict_put(headers, "Date", "value"));

    /* And now overwrite it again, for good measure. */
    fail_unless(s3header_dict_put(headers, "Date", "value"));

    s3header_dict_free(headers);
}
END_TEST

START_TEST (test_header_dict_iterate) {
    S3HeaderDict *headers = s3header_dict_new();
    S3HeaderDictIterator *iterator;
    S3Header *next;

    /* Put two values */
    fail_unless(s3header_dict_put(headers, "key1", "value1"));
    fail_unless(s3header_dict_put(headers, "key2", "value2"));

    /* Iterate */
    iterator = s3header_dict_iterator_new(headers);
    fail_if(iterator == NULL);

    /* Get the first value */
    next = s3header_dict_next(iterator);
    // fail_unless(strcmp())
    
    /* Get the next value */
    next = s3header_dict_next(iterator);
    // fail_unless(strcmp())

    /* No more values, should return NULL */
    fail_unless(s3header_dict_next(iterator) == NULL);

    /* Clean up */
    s3header_dict_iterator_free(iterator);
    s3header_dict_free(headers);
}
END_TEST

START_TEST (test_header_new) {
    S3Header *header = s3header_new("Date", "value");
    s3header_free(header);
}
END_TEST

START_TEST (test_header_values) {
    S3Header *header;
    S3List *values;
    S3ListIterator *i;

    header = s3header_new("Date", "value");
    values = s3header_values(header);

    i = s3list_iterator_new(values);    
    fail_unless(strcmp(s3list_iterator_next(i), "value") == 0);

    s3_release(i);
    s3header_free(header);
}
END_TEST

Suite *S3Header_suite(void) {
    Suite *s = suite_create("S3Header");

    TCase *tc_headers = tcase_create("Headers");
    suite_add_tcase(s, tc_headers);
    tcase_add_test(tc_headers, test_header_dict_new);
    tcase_add_test(tc_headers, test_header_dict_put);
    tcase_add_test(tc_headers, test_header_dict_iterate);    
    tcase_add_test(tc_headers, test_header_new);
    tcase_add_test(tc_headers, test_header_values);

    return s;
}
