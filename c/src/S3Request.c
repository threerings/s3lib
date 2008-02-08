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

static void s3request_dealloc (S3TypeRef object);

/**
 * S3 HTTP Request Context.
 * The request context exposes the URL, method, and headers of a composed S3 REST request.
 * The data can be passed to an HTTP client library to complete an S3 operation.
 *
 * @sa s3request_new()
 */
struct S3Request {
    S3RuntimeBase base;

    /** @internal
     * Request method */
    S3HTTPMethod method;

    /** @internal
     * Request bucket. */
    S3String *bucket;

    /** @internal
     * Request object. */
    S3String *object;

    /** @internal
     * Request headers */
    S3List *headers;
};

/**
 * @internal
 * S3Request Class Definition
 */
static S3RuntimeClass S3RequestClass = {
    .dealloc = s3request_dealloc
};

/**
 * Instantiate a new S3Request instance.
 *
 * @param method The request HTTP method.
 * @param bucket The S3 bucket
 * @param object An S3 object key
 * @param headers An list of S3Header values.
 * @return A new S3Request instance, or NULL on failure.
 *
 * @attention 
 */
S3_DECLARE S3Request *s3request_new (S3HTTPMethod method, S3String *bucket, S3String *object, S3List *headers) {
    S3Request *req;

    /* Allocate a new S3 Request. */
    req = s3_object_alloc(&S3RequestClass, sizeof(S3Request));
    if (req == NULL)
        return NULL;

    /* Request method */
    req->method = method;

    /* S3 Bucket */
    req->bucket = s3_retain(bucket);

    /* S3 Object */
    req->object = s3_retain(object);
    
    /* Headers */
    req->headers = s3_retain(headers);

    return (req);
}

/**
 * Return the request method.
 * @param request A S3Request instance.
 * @return The S3HTTPMethod to be used for this request.
 */
S3_EXTERN S3HTTPMethod s3request_method (S3Request *request) {
    return request->method;
}

/**
 * Return the request bucket.
 * @param request A S3Request instance.
 * @return The request S3 bucket.
 */
S3_EXTERN S3String *s3request_bucket (S3Request *request) {
    return request->bucket;
}

/**
 * Return the request S3 object.
 * @param request A S3Request instance.
 * @return The S3 object key for this request.
 */
S3_EXTERN S3String *s3request_object (S3Request *request) {
    return request->object;
}

/**
 * Return the request HTTP headers.
 * @param request A S3Request instance.
 * @return A list of S3Header values.
 */
S3_EXTERN S3List *s3request_headers (S3Request *request) {
    return request->headers;
}

/**
 * Sign the request.
 * @param request A S3Request instance.
 */
S3_EXTERN void s3request_sign (S3_UNUSED S3Request *request) {
    //S3Dict *allHeaders = s3dict_new();
    
}

/**
 * @internal
 *
 * S3Request deallocation callback.
 * @warning Do not call directly, use #s3_release
 * @param req A S3Request instance.
 */
static void s3request_dealloc (S3TypeRef object) {
    S3Request *req = (S3Request *) object;

    if (req->bucket != NULL)
        s3_release(req->bucket);

    if (req->object != NULL)
        s3_release(req->object);
        
    if (req->headers != NULL)
        s3_release(req->headers);
}

/*!
 * @} S3Request
 */
