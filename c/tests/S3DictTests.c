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

START_TEST (test_new) {
    S3Dict *dict = s3dict_new();
    s3_release(dict);
}
END_TEST

START_TEST (test_copy) {
    S3Dict *dict = s3_autorelease(s3dict_new());
    S3Dict *copy;

    s3dict_put(dict, S3STR("key"), S3STR("value"));
    copy = s3_autorelease( s3dict_copy(dict) );
    
    fail_unless(s3_equals(dict, copy));
}
END_TEST

START_TEST (test_equals_hash) {
    S3Dict *dict = s3_autorelease(s3dict_new());
    S3Dict *copy = s3_autorelease(s3dict_new());

    /* Should be unequal */
    s3dict_put(dict, S3STR("key"), S3STR("value"));
    s3dict_put(copy, S3STR("key"), S3STR("value"));

    fail_unless(s3_equals(dict, copy));
    fail_unless(s3_hash(dict) == s3_hash(copy));

    /* Should be unequal */
    s3dict_put(dict, S3STR("key2"), S3STR("val2"));
    s3dict_put(copy, S3STR("key2"), S3STR("notval"));

    fail_if(s3_equals(dict, copy));
    fail_if(s3_hash(dict) == s3_hash(copy));
}
END_TEST

START_TEST (test_put_get) {
    S3Dict *dict = s3_autorelease(s3dict_new());
    S3String *key;
    S3String *value;

    /* Put a value. */
    key = S3STR("key");
    value = S3STR("value");

    fail_unless(s3dict_put(dict, key, value));

    /* Get it back out again (use a different copy of the key, to ensure hashing works as expected) */
    key = s3_autorelease(s3string_new("key"));
    fail_if(s3dict_get(dict, key) == NULL);

    /* And now overwrite it, for good measure. */
    key = S3STR("key");
    value = S3STR("value");

    fail_unless(s3dict_put(dict, key, value));
}
END_TEST

START_TEST (test_iterate) {
    S3Dict *dict = s3_autorelease(s3dict_new());
    S3DictIterator *iterator;
    S3String *next;

    /* Example data */
    S3String *key1 = S3STR("key1");
    S3String *value1 = S3STR("value1");

    S3String *key2 = S3STR("key2");
    S3String *value2 = S3STR("value2");

    /* Put two values */
    fail_unless(s3dict_put(dict, key1, value1));
    fail_unless(s3dict_put(dict, key2, value2));

    /* Iterate */
    iterator = s3_autorelease(s3dict_iterator_new(dict));
    fail_if(iterator == NULL);

    /* Get the first value */
    fail_unless(s3dict_iterator_hasnext(iterator));
    next = s3dict_iterator_next(iterator);
    fail_unless(
            s3_equals(next, S3STR("key1")) ||
            s3_equals(next, S3STR("key2"))
    );
    
    /* Get the next value */
    fail_unless(s3dict_iterator_hasnext(iterator));
    next = s3dict_iterator_next(iterator);
    fail_unless(
            s3_equals(next, S3STR("key1")) ||
            s3_equals(next, S3STR("key2"))
    );

    /* No more values, hasnext() should return false, next() should return NULL */
    fail_if(s3dict_iterator_hasnext(iterator));
    fail_unless(s3dict_iterator_next(iterator) == NULL);
}
END_TEST

Suite *S3Dict_suite(void) {
    Suite *s = suite_create("S3Dict");

    TCase *tc_dict = tcase_create("Dict");
    suite_add_tcase(s, tc_dict);
    tcase_add_test(tc_dict, test_new);
    tcase_add_test(tc_dict, test_copy);
    tcase_add_test(tc_dict, test_equals_hash);
    tcase_add_test(tc_dict, test_put_get);
    tcase_add_test(tc_dict, test_iterate);    

    return s;
}
