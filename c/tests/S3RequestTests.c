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

#include "tests.h"

static S3Request *create_request () {
    S3Dict *headers;
    
    headers = s3_autorelease(s3dict_new());
    s3dict_put(headers, S3STR("test"), S3STR("value"));

    return s3_autorelease( s3request_new(S3_HTTP_PUT, S3STR("bucket"), S3STR("object"), headers, NULL) );
}

START_TEST (test_new) {
    S3Request *req = create_request();
    S3Dict *headers = s3request_headers(req);
    
    /* Verify that the Date header was set */
    S3String *date = s3dict_get(headers, S3STR("Date"));
    fail_if(date == NULL);
    fail_if(s3string_length(date) == 0);
}
END_TEST

START_TEST (test_bucket) {
    S3Request *req = create_request();
    fail_unless(s3_equals(s3request_bucket(req), S3STR("bucket")));
}
END_TEST

START_TEST (test_object) {
    S3Request *req = create_request();
    fail_unless(s3_equals(s3request_object(req), S3STR("object")));
}
END_TEST

START_TEST (test_method) {
    S3Request *req = create_request();
    fail_unless(s3request_method(req) == S3_HTTP_PUT);
}
END_TEST

START_TEST (test_headers) {
    S3Request *req = create_request();
    S3Dict *headers = s3request_headers(req);
    S3String *value = s3dict_get(headers, S3STR("test"));
    
    fail_unless(s3_equals(S3STR("value"), value));
}
END_TEST

START_TEST (test_policy) {
    S3Request   *request;
    S3Dict      *headers;
    S3String    *policy;
    S3String    *expected;
    time_t      expire = 1202554471; /* Sat 09 Feb 2008 10:54:31 GMT */

    /* Create the request */
    headers = s3_autorelease( s3dict_new() );
    s3dict_put(headers, S3STR("Content-Type"), S3STR("text/html"));
    s3dict_put(headers, S3STR("Content-MD5"), S3STR("CAFE"));
    s3dict_put(headers, S3STR("x-amz-meta-test"), S3STR("metadata"));
    request = s3_autorelease( s3request_new(S3_HTTP_DELETE, S3STR("bucket"), S3STR("object"), headers, &expire) );

    policy = s3request_policy(request);
    expected = S3STR(
        "DELETE\n"
        "CAFE\n"
        "text/html\n"
        "Sat 09 Feb 2008 10:54:31 GMT\n"
        "x-amz-meta-test:metadata\n"
        "/bucket/object\n"
    );

    fail_unless(s3_equals(policy, expected), "Returned policy invalid\nReturned:\n'%s'\nExpected:\n'%s'\n", s3string_cstring(policy), s3string_cstring(expected));
}
END_TEST

Suite *S3Request_suite(void) {
    Suite *s = suite_create("S3Request");

    TCase *tc_general = tcase_create("General");
    suite_add_tcase(s, tc_general);
    tcase_add_test(tc_general, test_new);
    tcase_add_test(tc_general, test_bucket);
    tcase_add_test(tc_general, test_object);
    tcase_add_test(tc_general, test_method);
    tcase_add_test(tc_general, test_headers);
    tcase_add_test(tc_general, test_policy);

    return s;
}
