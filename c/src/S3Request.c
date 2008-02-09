/*
 * S3Request.c vi:ts=4:sw=4:expandtab:
 * Amazon S3 Library
 *
 * Author: Landon Fuller <landonf@threerings.net>
 *
 * Copyright (c) 2005 - 2008 Landon Fuller <landonf@bikemonkey.org>
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

#include <openssl/hmac.h>
#include <openssl/evp.h>

#include "S3Lib.h"
#include "base64.h"

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

/** @internal Header prefix for generic S3 headers. */
static const char AMAZON_HEADER_PREFIX[] = "x-amz-";

/** @internal Header for S3's alternate date. */
static const char ALTERNATIVE_DATE_HEADER[] = "x-amz-date";

/** @internal Header for Amazon Authorization. */
static const char AMAZON_AUTHORIZATION_HEADER[] = "Authorization";

/* @internal SHA-1 message digest size */
#define CRYPTO_DIGEST_SHA1_SIZE 20    /* 160 bits */

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
     * Request headers (String, String) */
    S3Dict *headers;
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
 * @param headers A dictionary of S3String header names and values.
 * @return A new S3Request instance, or NULL on failure.
 *
 * @attention 
 */
S3_DECLARE S3Request *s3request_new (S3HTTPMethod method, S3String *bucket, S3String *object, S3Dict *headers) {
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
S3_DECLARE S3HTTPMethod s3request_method (S3Request *request) {
    return request->method;
}

/**
 * Return the request bucket.
 * @param request A S3Request instance.
 * @return The request S3 bucket.
 */
S3_DECLARE S3String *s3request_bucket (S3Request *request) {
    return request->bucket;
}

/**
 * Return the request S3 object.
 * @param request A S3Request instance.
 * @return The S3 object key for this request.
 */
S3_DECLARE S3String *s3request_object (S3Request *request) {
    return request->object;
}

/**
 * Return the request HTTP header dictionary.
 * @param request A S3Request instance.
 * @return A dictionary of S3String header name, value pairs.
 */
S3_DECLARE S3Dict *s3request_headers (S3Request *request) {
    return request->headers;
}

/**
 * @internal
 * Returns the current time as an RFC 822 formatted string,
 * or NULL if a failure occurs.
 */
static S3String *rfc822_now () {
    /* RFC 822 format; '%a %d %b %Y %T GMT' -- Maximum size of 29 bytes */
    char buf[29];
    time_t now;
    struct tm gmt;
        
    /* Get the current time */
    if (time(&now) == (time_t)-1) {
        DEBUG("time() failed, returned %jd", (intmax_t) now);
        return NULL;   
    }

    if (gmtime_r(&now, &gmt) == NULL) {
        DEBUG("gmtime() failed");
        return NULL;
    }

    /* Write out the date string */
    if (strftime(buf, sizeof(buf), "%a %d %b %Y %T GMT", &gmt) == 0) {
        DEBUG("strftime() failed, returned 0");
        return NULL;
    }

    return s3_autorelease( s3string_new(buf) );
}

/**
 * @internal
 * Return the HTTP method string for a given S3HTTPMethod. Used for S3
 * request signing.
 */
static const char *s3request_http_verb (S3HTTPMethod method) {
    /* HTTP-Verb */
    switch (method) {
        case S3_HTTP_PUT:
            return "PUT";
        case S3_HTTP_GET:
            return "GET";
        case S3_HTTP_HEAD:
            return "HEAD";
        case S3_HTTP_DELETE:
            return "DELETE";
    }

    DEBUG("BUG: Missing HTTP method");
    assert(0);
    return "";
}

/**
 * Sign the request. Any missing, mandatory headers (eg, Date) will be added to
 * the request.
 *
 * @param request A S3Request instance.
 */
S3_DECLARE void s3request_sign (S3Request *req, S3String *awsId, S3String *awsKey) {
    S3AutoreleasePool *pool = s3autorelease_pool_new();

    /* Header keys */
    S3String *amz_prefix =  S3STR(AMAZON_HEADER_PREFIX);
    S3String *content_md5 = S3STR("content-md5");
    S3String *content_type = S3STR("content-type");
    S3String *date_header_str = S3STR("date");
    
    /*
     * Add all headers needing signing to the signed_headers dictionary
     */
    S3Dict *signed_headers = s3_autorelease( s3dict_new() );
    S3List *amz_headers = s3_autorelease ( s3list_new() );
    {
        S3DictIterator *i = s3_autorelease( s3dict_iterator_new(req->headers) );

        /* Estimated size of the signing string buffer. TODO: Guess the size of the HTTP-Request-URI */
        size_t size_estimate = 0;

        while (s3dict_iterator_hasnext(i)) {
            S3String *key = s3dict_iterator_next(i);
            S3String *name = s3string_lowercase(key);
            S3String *value = s3dict_get(req->headers, key);
            
            assert(value != NULL);

            /* Increase the estimate size of the signing buffer by length(value) + '\n' */
            size_estimate += s3string_length(value) + 1;

            /* x-amz- headers (must be sorted ) */
            if (s3string_startswith(name, amz_prefix)) {
                s3dict_put(signed_headers, name, value);
                s3list_append(amz_headers, key);

            /* content-md5 header */
            } else if (s3_equals(name, content_md5)) {
                s3dict_put(signed_headers, name, value);

            /* content-type header */
            } else if (s3_equals(name, content_type)) {
                s3dict_put(signed_headers, name, value);

            /* date header */
            } else if (s3_equals(name, date_header_str)) {
                s3dict_put(signed_headers, name, value);
            }
        }

        /* Add a Date header, if necessary */
        if (s3dict_get(signed_headers, date_header_str) == NULL) {
            S3String *date;

            date = rfc822_now();
            assert(date != NULL);

            s3dict_put(req->headers, date_header_str, date);
            s3dict_put(signed_headers, date_header_str, date);
        }

        /* Fill in missing mandatory headers with blank strings */
        S3String *blank = S3STR("");

        if (s3dict_get(signed_headers, content_md5) == NULL)
            s3dict_put(signed_headers, content_md5, blank);

        if (s3dict_get(signed_headers, content_type) == NULL)
            s3dict_put(signed_headers, content_type, blank);
            
        /* Sort the AMZ headers lexicographically */
        s3list_sort(amz_headers, &s3list_lexicographical_compare, NULL);
    }


    /*
     * HMAC the request
     */
    {
        HMAC_CTX        hmac;
        S3String        *str;
        const EVP_MD    *md;
        
        unsigned char   output[CRYPTO_DIGEST_SHA1_SIZE];
        unsigned int    output_len;

        const unsigned char newline[] = "\n";
        const unsigned char slash[] = "/";
        const unsigned char colon[] = ":";

        /* Set up the context */
        md = EVP_sha1();
        HMAC_CTX_init(&hmac);
        HMAC_Init_ex(&hmac, s3string_cstring(awsKey), s3string_length(awsKey), md, NULL);

        /* HTTP-Verb */
        HMAC_Update(&hmac, (const unsigned char *) s3request_http_verb(req->method), strlen(s3request_http_verb(req->method)));
        HMAC_Update(&hmac, newline, sizeof(newline));

        /* Content-MD5 */
        str = s3dict_get(signed_headers, content_md5);
        HMAC_Update(&hmac, (const unsigned char *) s3string_cstring(str), s3string_length(str));
        HMAC_Update(&hmac, newline, sizeof(newline));
    
        /* Content-Type */
        str = s3dict_get(signed_headers, content_type);
        HMAC_Update(&hmac, (const unsigned char *) s3string_cstring(str), s3string_length(str));
        HMAC_Update(&hmac, newline, sizeof(newline));
        
        /* Date */
        str = s3dict_get(signed_headers, date_header_str);
        HMAC_Update(&hmac, (const unsigned char *) s3string_cstring(str), s3string_length(str));
        HMAC_Update(&hmac, newline, sizeof(newline));
                
        /* Canonicalized AMZ Headers */
        S3ListIterator *i = s3list_iterator_new(amz_headers);
        while (s3list_iterator_hasnext(i)) {
            S3String *key = s3string_lowercase( s3list_iterator_next(i) );
            S3String *value = s3dict_get(signed_headers, key);
            
            assert(value != NULL);
            
            HMAC_Update(&hmac, (const unsigned char *) s3string_cstring(key), s3string_length(key));
            HMAC_Update(&hmac, colon, sizeof(colon));
            HMAC_Update(&hmac, (const unsigned char *) s3string_cstring(value), s3string_length(value));
            HMAC_Update(&hmac, newline, sizeof(newline));
        }

        /* Canonicalized Resource */
        HMAC_Update(&hmac, slash, sizeof(slash));
        HMAC_Update(&hmac, (const unsigned char *) s3string_cstring(req->bucket), s3string_length(req->bucket));
        HMAC_Update(&hmac, slash, sizeof(slash));
        HMAC_Update(&hmac, (const unsigned char *) s3string_cstring(req->object), s3string_length(req->object));

        /* TODO: sub-resource, if present -- "?acl", "?location", "?logging", or "?torrent" */

        /* Output the HMAC */
        output_len = sizeof(output);
        HMAC_Final(&hmac, output, &output_len);
        HMAC_CTX_cleanup(&hmac);
        
        /* Create the authorization header */
        {
            char *b64;
            char *authstring;

            /* Base64 the signature */
            if (s3_base64_encode(output, output_len, &b64) == -1) {
                // XXX report error
                goto cleanup;
            }

            /* Create the authorization header */
            asprintf(&authstring, "AWS %s:%s", s3string_cstring(awsId), b64);
            if (authstring == NULL) {
                // XXX report error
                goto cleanup;
            }
            s3dict_put(req->headers, S3STR(AMAZON_AUTHORIZATION_HEADER), S3STR(authstring));
        }

    }


cleanup:
    /* Free our pool */
    s3_release(pool);
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
