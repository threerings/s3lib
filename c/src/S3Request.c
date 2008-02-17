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

/** @internal Header for Amazon Authorization. */
static const char AMAZON_AUTHORIZATION_HEADER[] = "Authorization";

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
    .version = S3_CURRENT_RUNTIME_CLASS_VERSION,
    .dealloc = s3request_dealloc
};

/**
 * @internal
 * Returns the provided time as an RFC 822 formatted string, or NULL if a failure occurs.
 *
 * @param reqtime Time to format. If NULL, the current time will be used.
 */
static S3String *rfc822_time (const time_t *reqtime) {
    /* RFC 822 format; '%a %d %b %Y %T GMT' -- Maximum size of 29 bytes */
    char buf[29];
    time_t now;
    struct tm gmt;

    /* Expiration */
    if (reqtime == NULL) {
        /* Get the current time */
        if (time(&now) == (time_t)-1) {
            DEBUG("time() failed, returned %jd", (intmax_t) now);
            return NULL;
        }
    } else {
        now = *reqtime;
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
 * Instantiate a new S3Request instance.
 *
 * @param method The request HTTP method.
 * @param bucket The S3 bucket
 * @param object An S3 object key
 * @param headers A dictionary of S3String HTTP header names and values.
 * @param expire The date at which the request will expire, as seconds since the epoch. If set to NULL, the current time will be used.
 *
 * @return A new S3Request instance, or NULL on failure.
 *
 * @attention The @a expire argument will be used to set the HTTP Date header.
 *
 * @note Amazon S3 handles the expiration date differently depending on the authentication method. In the case of
 * header authentication, the request must be made within 15 minutes of the @a expire time. In the case of query
 * string authentication, the request must be made BEFORE the @a expire time has been reached. For more information,
 * see http://docs.amazonwebservices.com/AmazonS3/2006-03-01/
 */
S3_DECLARE S3Request *s3request_new (S3HTTPMethod method, S3String *bucket, S3String *object, S3Dict *headers, const time_t *expire) {
    S3AutoreleasePool *pool = s3autorelease_pool_new();
    S3Request *req;
    S3String *date;

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
    req->headers = s3dict_copy(headers);
    if (req->headers == NULL)
        goto cleanup;

    /* Expiration */
    date = rfc822_time(expire);
    if (date == NULL)
        goto cleanup;

    s3dict_put(req->headers, S3STR("Date"), date);

    s3_release(pool);
    return (req);

cleanup:
    s3_release(req);
    s3_release(pool);

    return NULL;
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
 * Return the request's policy -- a canonical string description of the request,
 * as per http://docs.amazonwebservices.com/AmazonS3/2006-03-01/RESTAuthentication.html
 * to be used for signing and authenticating the request.
 *
 * @param req A S3Request instance.
 */
S3_DECLARE S3String *s3request_policy (S3Request *req) {
    S3AutoreleasePool *pool = s3autorelease_pool_new();
    S3String *result;
    
    /* Size estimate */
    size_t policy_size = 0;

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

        while (s3dict_iterator_hasnext(i)) {
            S3String *key = s3dict_iterator_next(i);
            S3String *name = s3string_lowercase(key);
            S3String *value = s3dict_get(req->headers, key);
            bool amz_header = false;
            
            assert(value != NULL);
            
            
            if (s3string_startswith(name, amz_prefix)) {
                /* AMZ Header */
                amz_header = true;
                s3list_append(amz_headers, key);
                s3dict_put(signed_headers, name, value);
                
                /* Each entry <name>:<value>\n */
                policy_size += s3string_length(name) + 1 + s3string_length(value) + 1;

            } else if (s3_equals(name, content_md5) ||
                s3_equals(name, content_type) ||
                s3_equals(name, date_header_str))
            {
                /* Mandatory header */
                s3dict_put(signed_headers, name, value);
    
                /* Each entry <value>\n */
                policy_size += s3string_length(value) + 1;
            }
        }

        /* Fill in missing mandatory headers with blank strings */
        S3String *blank = S3STR("");

        if (s3dict_get(signed_headers, content_md5) == NULL) {
            s3dict_put(signed_headers, content_md5, blank);
            policy_size += 1; // for \n
        }

        if (s3dict_get(signed_headers, content_type) == NULL) {
            s3dict_put(signed_headers, content_type, blank);
            policy_size += 1; // for \n            
        }

        /* Sort the AMZ headers lexicographically */
        s3list_sort(amz_headers, &s3list_lexicographical_compare, NULL);
    }
    
    /* Add the resource to the size estimate: / + bucket + / + resource + \n */
    policy_size += 1 + s3string_length(req->bucket) + 1 + s3string_length(req->object) + 1;
    
    /* Add extra room for any HTTP-Verb to the size estimate: verb + \n */
    policy_size += 10;

    /*
     * Build the output string.
     */
    S3StringBuilder *builder = s3stringbuilder_new(policy_size);
    {
        S3String *fixed_header_string;
        S3String *resource_string;
        
        /* Fixed header portion */
        fixed_header_string = s3string_withformat(
            "%s\n"  // HTTP-Verb
            "%s\n"  // Content-MD5
            "%s\n"  // Content-Type
            "%s\n", // Date
            s3request_http_verb(req->method),
            s3string_cstring( s3dict_get(signed_headers, content_md5) ),
            s3string_cstring( s3dict_get(signed_headers, content_type) ),
            s3string_cstring( s3dict_get(signed_headers, date_header_str) )
        );
        s3stringbuilder_append(builder, fixed_header_string);

        /* Canonicalized AMZ Headers */
        S3ListIterator *i = s3_autorelease( s3list_iterator_new(amz_headers) );
        while (s3list_iterator_hasnext(i)) {
            S3String *key = s3string_lowercase( s3list_iterator_next(i) );
            S3String *value = s3dict_get(signed_headers, key);
            S3String *amz_header_string;

            assert(value != NULL);
            amz_header_string = s3string_withformat("%s:%s\n", s3string_cstring(key), s3string_cstring(value));
            s3stringbuilder_append(builder, amz_header_string);
        }

        /* Canonicalized Resource */
        resource_string = s3string_withformat("/%s/%s\n", s3string_cstring(req->bucket), s3string_cstring(req->object));
        s3stringbuilder_append(builder, resource_string);

        /* TODO: sub-resource, if present -- "?acl", "?location", "?logging", or "?torrent" */
    }
    
    s3_release(pool);

    /* Fetch the result and release our builder */
    result = s3stringbuilder_string(builder);
    s3_release(builder);

    /* Print a debugging message if our estimate is invalid */
    if (policy_size < s3string_length(result)) {
        DEBUG("Estimated result size was too low (estimated %zu, actual %zu)", policy_size, s3string_length(result));
    }

    return (result);
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
