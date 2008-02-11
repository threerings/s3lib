/*
 * S3Request.h vi:ts=4:sw=4:expandtab:
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

#ifndef S3REQUEST_H
#define S3REQUEST_H

/**
 * @file
 * @brief S3 HTTP request context.
 * @author Landon Fuller <landonf@threerings.net>
 */

/*!
 * @addtogroup S3Request
 * @{
 */

/* S3 HTTP Request Context. */
typedef struct S3Request S3Request;

/**
 * HTTP Request Methods.
 * All HTTP methods used to implement the S3 REST API.
 */
typedef enum {
    /** HTTP PUT Request. */
    S3_HTTP_PUT,

    /** HTTP GET Request. */
    S3_HTTP_GET,

    /** HTTP HEAD Request. */
    S3_HTTP_HEAD,

    /** HTTP DELETE Request. */
    S3_HTTP_DELETE
} S3HTTPMethod;

S3_EXTERN S3Request *s3request_new (S3HTTPMethod method, S3String *bucket, S3String *resource, S3Dict *headers);
S3_EXTERN S3HTTPMethod s3request_method (S3Request *);
S3_EXTERN S3String *s3request_bucket (S3Request *);
S3_EXTERN S3String *s3request_object (S3Request *);
S3_EXTERN S3Dict *s3request_headers (S3Request *);
S3_EXTERN S3String *s3request_policy (S3Request *req);

/*!
 * @} S3Library
 */

#endif /* S3REQUEST_H */
