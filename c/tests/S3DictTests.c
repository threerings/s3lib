/*
 * S3DictTests.c vi:ts=4:sw=4:expandtab:
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

START_TEST (test_dict_new) {
    S3Dict *dict = s3dict_new();
    s3_release(dict);
}
END_TEST

START_TEST (test_dict_put_get) {
    S3Dict *dict = s3dict_new();
    S3String *key;
    S3String *value;

    /* Put a value. */
    key = s3string_new("key");
    value = s3string_new("value");

    fail_unless(s3dict_put(dict, key, value));

    s3_release(key);
    s3_release(value);

    /* Get it back out again */
    key = s3string_new("key");

    fail_if(s3dict_get(dict, key) == NULL);

    s3_release(key);

    /* And now overwrite it again, for good measure. */
    key = s3string_new("key");
    value = s3string_new("value");

    fail_unless(s3dict_put(dict, key, value));

    s3_release(key);
    s3_release(value);

    /* All done */
    s3_release(dict);
}
END_TEST

START_TEST (test_dict_iterate) {
    S3Dict *dict = s3dict_new();
    S3DictIterator *iterator;
    S3String *next;

    /* Example data */
    S3String *key1 = s3string_new("key1");
    S3String *value1 = s3string_new("value1");

    S3String *key2 = s3string_new("key2");
    S3String *value2 = s3string_new("value2");

    /* Put two values */
    fail_unless(s3dict_put(dict, key1, value1));
    fail_unless(s3dict_put(dict, key2, value2));

    /* Iterate */
    iterator = s3dict_iterator_new(dict);
    fail_if(iterator == NULL);

    /* Get the first value */
    next = s3dict_iterator_next(iterator);
    fail_unless(
            strcmp(s3string_cstring(next), "key1") == 0 ||
            strcmp(s3string_cstring(next), "key2") == 0
    );
    
    /* Get the next value */
    next = s3dict_iterator_next(iterator);
    fail_unless(
            strcmp(s3string_cstring(next), "key1") == 0 ||
            strcmp(s3string_cstring(next), "key2") == 0
    );

    /* No more values, should return NULL */
    fail_unless(s3dict_iterator_next(iterator) == NULL);

    /* Clean up */
    s3_release(key1);
    s3_release(value1);

    s3_release(key2);
    s3_release(value2);

    s3_release(iterator);
    s3_release(dict);
}
END_TEST

Suite *S3Dict_suite(void) {
    Suite *s = suite_create("S3Dict");

    TCase *tc_dict = tcase_create("Dict");
    suite_add_tcase(s, tc_dict);
    tcase_add_test(tc_dict, test_dict_new);
    tcase_add_test(tc_dict, test_dict_put_get);
    tcase_add_test(tc_dict, test_dict_iterate);    

    return s;
}
