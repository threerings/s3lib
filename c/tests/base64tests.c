/*
 * Copyright (c) 1999 - 2001 Kungliga Tekniska Högskolan
 * (Royal Institute of Technology, Stockholm, Sweden).
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */


#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include "tests.h"

#include <string.h>
#include <stdlib.h>

#include <src/base64.h>

START_TEST (test_encode) {
    int numtest = 1;
    struct test {
        void *data;
        size_t len;
        const char *result;
    } *t, tests[] = {
        { "", 0 , "" },
        { "1", 1, "MQ==" },
        { "22", 2, "MjI=" },
        { "333", 3, "MzMz" },
        { "4444", 4, "NDQ0NA==" },
        { "55555", 5, "NTU1NTU=" },
        { "abc:def", 7, "YWJjOmRlZg==" },
        { NULL, 0, NULL }
    };

    for(t = tests; t->data; t++) {
        char *str;
        size_t len;

        len = s3_base64_encode(t->data, t->len, &str);
        fail_unless(strcmp(str, t->result) == 0, "failed test %d: %s != %s\n", numtest, str, t->result);
        free(str);

        str = strdup(t->result);
        len = s3_base64_decode(t->result, str);
        fail_unless(len == t->len, "failed test %d: len %d != %d\n", numtest, len, t->len);
        fail_unless(memcmp(str, t->data, t->len) == 0, "failed test %d: data\n", numtest);
        free(str);

        numtest++;
    }

    {
        char str[32];
        fail_unless(s3_base64_decode("M=M=", str) == -1, "failed test %d: successful decode of `M=M='\n", numtest++);
	    fail_unless(s3_base64_decode("MQ===", str) == -1, "failed test %d: successful decode of `MQ==='\n", numtest++);
    }
}
END_TEST

Suite *base64_suite(void) {
    Suite *s = suite_create("Base64");

    TCase *tc_general = tcase_create("General");
    suite_add_tcase(s, tc_general);
    tcase_add_test(tc_general, test_encode);

    return s;
}
