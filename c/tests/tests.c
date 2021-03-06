/*
 * tests.c vi:ts=4:sw=4:expandtab:
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
#endif /* HAVE_CONFIG_H */

#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>

#include <curl/curl.h>
#include <check.h>

#include <tests.h>

void print_usage(const char *name) {
    printf("Usage: %s [filename]\n", name);
    printf(" [filename]\tWrite XML log to <filename>\n");
}

int main(int argc, char *argv[]) {
    Suite *s;
    SRunner *sr;
    int nf;

    if (argc > 2) {
        print_usage(argv[0]);
        exit(1);
    }

    /* Load all test suites */
    s = S3AutoreleasePool_suite();
    sr = srunner_create(s);
    srunner_add_suite(sr, S3Account_suite());
    srunner_add_suite(sr, S3Atomic_suite());
    srunner_add_suite(sr, S3Connection_suite());
    srunner_add_suite(sr, S3Dict_suite());
    srunner_add_suite(sr, S3Error_suite());
    srunner_add_suite(sr, S3Lib_suite());
    srunner_add_suite(sr, S3List_suite());
    srunner_add_suite(sr, S3Request_suite());
    srunner_add_suite(sr, S3String_suite());
    srunner_add_suite(sr, S3StringBuilder_suite());
    srunner_add_suite(sr, base64_suite());

    /* Enable XML output */
    if (argc == 2)
        srunner_set_xml(sr, argv[1]);

    /* Library Initializers */
    s3lib_global_init();
    s3lib_enable_debugging(true); // XXX for now
    curl_global_init(CURL_GLOBAL_ALL);

    /* Fallback autorelease pool */
    S3AutoreleasePool *pool = s3autorelease_pool_new();

    /* Run tests */
    srunner_run_all(sr, CK_NORMAL);

    /* Clear the pool */
    s3_release(pool);

    nf = srunner_ntests_failed(sr);
    srunner_free(sr);

    /* Library cleanup */
    s3lib_global_cleanup();

    if (nf == 0)
        exit(EXIT_SUCCESS);
    else
        exit(EXIT_FAILURE);
}
