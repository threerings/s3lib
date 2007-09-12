/*
 * S3Request.c vi:ts=4:sw=4:expandtab:
 * Amazon S3 Library
 *
 * Author: Landon Fuller <landonf@threerings.net>
 *
 * Copyright (c) 2007 Landon Fuller <landonf@bikemonkey.org>
 * Copyright (c) 2007 Three Rings Design, Inc.
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

#include <assert.h>
#include <stdlib.h>
#include <string.h>

#include "S3Lib.h"

/**
 * @file
 * @brief S3 HTTP request context.
 * @author Landon Fuller <landonf@threerings.net>
 */

/**
 * @defgroup S3Request HTTP Request Context
 * @ingroup S3Library
 * @{
 */

/**
 * S3 HTTP Request Context.
 * The request context exposes the URL, method, and headers of a composed S3 REST request.
 * The data can be passed to an HTTP client library to complete an S3 operation.
 *
 * @sa s3request_new()
 */
struct S3Request {
    /** @internal
     * Request URL. */
    safestr_t url;

    /** @internal
     * Request method */
    S3HTTPMethod method;

    /** @internal
     * Request headers */
    S3HeaderDict *headers;
};


/**
 * Instantiate a new S3Request instance.
 *
 * @param url An S3 resource URL.
 * @param method The request HTTP method.
 * @return A new S3Request instance, or NULL on failure.
 */
S3_DECLARE S3Request *s3request_new (const char *url, S3HTTPMethod method) {
    S3Request *req;

    /* Allocate a new S3 Request. */
    req = calloc(1, sizeof(S3Request));
    if (req == NULL)
        return NULL;

    /* S3 Resource */
    req->url = s3_safestr_create(url, SAFESTR_IMMUTABLE);

    /* Request method */
    req->method = method;

    return (req);
}


/**
 * Free a S3Request instance.
 * @param req A S3Request instance.
 */
S3_DECLARE void s3request_free (S3Request *req) {
    if (req->url != NULL)
        safestr_release(req->url);

    free(req);
}

/*!
 * @} S3Request
 */
