/*
 * S3StringBuilder.c vi:ts=4:sw=4:expandtab:
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

START_TEST (test_new) {
    S3StringBuilder *builder = s3stringbuilder_new(0);
    s3_release(builder);
}
END_TEST

START_TEST (test_string) {
    S3StringBuilder *builder = s3_autorelease( s3stringbuilder_new(1) );
    S3String *string = S3STR("test");
    
    s3stringbuilder_append(builder, string);
    fail_unless(s3_equals(s3stringbuilder_string(builder), string));
}
END_TEST

START_TEST (test_length) {
    S3StringBuilder *builder = s3_autorelease( s3stringbuilder_new(20) );
    
    fail_unless(s3stringbuilder_length(builder) == 0);
    s3stringbuilder_append(builder, S3STR("test"));
    fail_unless(s3stringbuilder_length(builder) == 4);
}
END_TEST

START_TEST (test_append) {
    S3StringBuilder *builder = s3_autorelease( s3stringbuilder_new(3) );
    s3stringbuilder_append(builder, S3STR("Hello"));
    s3stringbuilder_append(builder, S3STR(" World"));    
    fail_unless(s3_equals(s3stringbuilder_string(builder), S3STR("Hello World")));
}
END_TEST

START_TEST (test_hash) {
    S3StringBuilder *hello = s3_autorelease( s3stringbuilder_new(0) );
    S3StringBuilder *helloagain = s3_autorelease( s3stringbuilder_new(0) );
    S3StringBuilder *bye = s3_autorelease( s3stringbuilder_new(0) );

    s3stringbuilder_append(hello, S3STR("Hello"));
    
    s3stringbuilder_append(helloagain, S3STR("Hel"));
    s3stringbuilder_append(helloagain, S3STR("lo"));
    
    s3stringbuilder_append(bye, S3STR("Bye"));

    fail_unless(s3_hash(hello) == s3_hash(helloagain));
    fail_if(s3_hash(hello) == s3_hash(bye));
}
END_TEST

START_TEST (test_equals) {
    S3StringBuilder *hello = s3_autorelease( s3stringbuilder_new(0) );
    S3StringBuilder *helloagain = s3_autorelease( s3stringbuilder_new(0) );
    S3StringBuilder *bye = s3_autorelease( s3stringbuilder_new(0) );

    s3stringbuilder_append(hello, S3STR("Hello"));
    
    s3stringbuilder_append(helloagain, S3STR("Hel"));
    s3stringbuilder_append(helloagain, S3STR("lo"));        

    s3stringbuilder_append(bye, S3STR("Bye"));

    fail_unless(s3_equals(hello, helloagain));
    fail_if(s3_equals(hello, bye));
}
END_TEST

Suite *S3StringBuilder_suite(void) {
    Suite *s = suite_create("S3StringBuilder");

    TCase *tc_general = tcase_create("General");
    suite_add_tcase(s, tc_general);
    tcase_add_test(tc_general, test_new);
    tcase_add_test(tc_general, test_string);
    tcase_add_test(tc_general, test_length);
    tcase_add_test(tc_general, test_append);
    tcase_add_test(tc_general, test_hash);
    tcase_add_test(tc_general, test_equals);

    return s;
}
