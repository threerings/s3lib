/*
 * S3Error.c vi:ts=4:sw=4:expandtab:
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

static const char error_document[] =
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" \
" <Error>" \
"   <Code>NoSuchKey</Code>" \
"   <Message>The resource you requested does not exist</Message>" \
"   <Resource>/mybucket/myfoto.jpg</Resource>" \
"   <RequestId>4442587FB7D0A2F9</RequestId>" \
" </Error>";

START_TEST (test_new) {
    S3ServerError *error;

    /* Parse the error. */
    error = s3server_error_new(S3STR(error_document));

    /* Check the request id. */
    fail_if(s3server_error_requestid(error) == NULL);
    fail_unless(strcmp(s3string_cstring(s3server_error_requestid(error)), "4442587FB7D0A2F9") == 0);

    s3_release(error);
}
END_TEST

Suite *S3Error_suite(void) {
    Suite *s = suite_create("S3Error");

    TCase *tc_general = tcase_create("General");
    suite_add_tcase(s, tc_general);
    tcase_add_test(tc_general, test_new);

    return s;
}
