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
    S3List *headers;
    S3Header *header;
    
    headers = s3_autorelease(s3list_new());
    header = s3_autorelease(s3header_new(S3STR("test"), S3STR("value")));
    s3list_append(headers, header);

    return s3request_new(S3_HTTP_PUT, S3STR("bucket"), S3STR("object"), headers);
}

START_TEST (test_new) {
    S3Request *req = create_request();
    s3_release(req);
}
END_TEST

START_TEST (test_bucket) {
    S3Request *req = create_request();
    fail_unless(s3_equals(s3request_bucket(req), S3STR("bucket")));
    s3_release(req);
}
END_TEST

START_TEST (test_object) {
    S3Request *req = create_request();
    fail_unless(s3_equals(s3request_object(req), S3STR("object")));
    s3_release(req);
}
END_TEST

START_TEST (test_method) {
    S3Request *req = create_request();
    fail_unless(s3request_method(req) == S3_HTTP_PUT);
    s3_release(req);
}
END_TEST

START_TEST (test_headers) {
    S3Request *req = create_request();
    fail_unless(s3request_method(req) == S3_HTTP_PUT);
    s3_release(req);
}
END_TEST

START_TEST (test_sign) {
    S3Request *req = create_request();
    
    s3request_sign(req);

    s3_release(req);
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
    tcase_add_test(tc_general, test_sign);

    return s;
}