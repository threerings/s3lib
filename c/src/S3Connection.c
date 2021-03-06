/*
 * S3Connection.c vi:ts=4:sw=4:expandtab:
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

#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include "S3Lib.h"

/**
 * @file
 * @brief S3 connection management.
 * @author Landon Fuller <landonf@threerings.net>
 */

/**
 * @defgroup S3Connection S3 Client Connection
 * @ingroup S3Library
 * @{
 */

/** Default Amazon S3 URL. XXX replace with method, should not be an extern global */
S3_DECLARE const char S3_DEFAULT_URL[] = "https://s3.amazonaws.com";

static void s3connection_dealloc (S3TypeRef obj);

/**
 * Maintains S3 connection state.
 * @attention Operations on a S3Connection instance are not thread-safe, and
 * a S3Connection should not be shared between threads without external synchronization.
 */
struct S3Connection {
    S3RuntimeBase base;

    /** @internal
     * AWS Account */
    S3Account *account;

    /** @internal
     * S3 server url. */
    S3String *s3_url;

    /** @internal
     * Cached cURL handle (not thread-safe). */
    CURL *handle;
};

/**
 * @internal
 * S3Connection Class Definition
 */
static S3RuntimeClass S3ConnectionClass = {
    .version = S3_CURRENT_RUNTIME_CLASS_VERSION,
    .dealloc = s3connection_dealloc
};

/**
 * Instantiate a new #S3Connection instance.
 * @attention Instances of #S3Connection are not re-entrant, and should not be
 * shared between multiple threads.
 *
 * @param account Amazon AWS Account.
 * @return A new #S3Connection instance, or NULL on failure.
 */
S3_DECLARE S3Connection *s3connection_new (S3Account *account) {
    S3Connection *conn;

    /* Allocate a new S3 Connection. */
    conn = s3_object_alloc(&S3ConnectionClass, sizeof(S3Connection));
    if (conn == NULL)
        return NULL;

    /* AWS Account */
    conn->account = s3_retain(account);

    /* Default S3 URL */
    conn->s3_url = s3string_new(S3_DEFAULT_URL);

    /* cURL handle */
    conn->handle = curl_easy_init();
    if (conn->handle == NULL)
        goto error;

    return (conn);    

error:
    s3_release(conn);
    return NULL;
}

/**
 * Set the #S3Connection's S3 service URL.
 *
 * The service URL defaults to #S3_DEFAULT_URL, and will not generally need to be changed.
 *
 * @param conn A valid #S3Connection instance.
 * @param s3_url The new S3 service URL.
 * @return true on success, false on failure.
 */
S3_DECLARE bool s3connection_set_url (S3Connection *conn, S3String *s3_url) {
    /* Release the old URL. */
    if (conn->s3_url != NULL)
        s3_release(conn->s3_url);

    /* Retain the new URL */
    conn->s3_url = s3_retain(s3_url);

    /* Success */
    return true;
}

#if DEAD_CODE // DEAD CODE
/**
 * @internal
 * Reset the S3Connection's CURL handle for a new request.
 *
 * @param conn A valid S3Connection instance.
 */
static void s3connection_reset_curl (S3Connection *conn) {
    curl_easy_reset(conn->handle);

    /* Restore any settings */
    if (s3lib_debugging())
        curl_easy_setopt(conn->handle, CURLOPT_VERBOSE, 1);
}
#endif

/**
 * Create a new S3 bucket
 *
 * @param conn A valid S3Connection instance.
 * @param bucketName The name of the bucket to create.
 * @return A #s3error_t result.
 */
S3_DECLARE void *s3connection_create_bucket (S3Connection *conn, S3_UNUSED S3String *bucketName) {
    CURLcode error;

#if DEAD_CODE
    if (s3curl_create_bucket(conn, bucketName, &error) == NULL)
        return NULL;
#endif

    error = curl_easy_perform(conn->handle);
    if (error != CURLE_OK) {
        fprintf(stderr, "Failure: %s\n", curl_easy_strerror(error));
    } else {
        fprintf(stderr, "Success");        
    }

    return NULL;
}

/**
 * @internal
 *
 * #S3Connection deallocation callback.
 * @warning Do not call directly, use #s3_release
 * @param conn A #S3Connection instance.
 */
static void s3connection_dealloc (S3TypeRef obj) {
    S3Connection *conn = (S3Connection *) obj;

    if (conn->account != NULL)
        s3_release(conn->account);

    if (conn->s3_url != NULL)
        s3_release(conn->s3_url);

    if (conn->handle != NULL) 
        curl_easy_cleanup(conn->handle);
}

#if DEAD_CODE
/**
 * @defgroup S3Curl S3 libcurl API
 *
 * Expert API providing access to the underlying cURL library.
 *
 * @warning Returned CURL handles are owned by the provided S3Connection,
 * and are only valid until the next function is called for the S3Connection instance.
 *
 * @{
 */

/**
 * Prepare and return the S3Connection's CURL handle for an S3 create bucket request.
 *
 * @param conn A valid S3Connection instance.
 * @param bucketName The name of the bucket to create.
 * @param error On failure, will contain the corresponding curl error code.
 * @return A borrowed reference to a configured CURL handle, or NULL on failure.
 * If failure occurs, the CURL error code will be stored in \a error.
 */
S3_DECLARE CURL *s3curl_create_bucket (S3Connection *conn, S3String *bucketName, CURLcode *error) {
    safestr_t url;
    safestr_t resource;
    char *escaped;
    uint32_t index;

    /* Reset the handle */
    curl_easy_reset(conn->handle);
    
    s3connection_reset_curl(conn);

    /* Set the request type */
    if ((*error = curl_easy_setopt(conn->handle, CURLOPT_UPLOAD, 1)) != CURLE_OK)
        return NULL;

    /* Create and set the resource URL. */
    escaped = curl_easy_escape(conn->handle, s3string_cstring(bucketName), 0);
    resource = s3_safestr_create(escaped, SAFESTR_IMMUTABLE);
    curl_free(escaped);
    
    /* Allocate a string large enough for s3_url + "/" (possibly) + resource */
    index = safestr_length(s3string_safestr(conn->s3_url));

    /* Set the base URL */
    url = safestr_alloc(index + 1 + safestr_length(resource), 0);
    safestr_append(&url, s3string_safestr(conn->s3_url));
    
    /* Check for, and append, if necessary, a trailing / */
    if (safestr_charat(url, index - 1) != '/')
        safestr_append(&url, SAFESTR_TEMP("/"));

    /* Append the resource */
    safestr_append(&url, resource);

    DEBUG("Configuring handle with URL: %s", s3_safestr_char(url));

    *error = curl_easy_setopt(conn->handle, CURLOPT_URL, url);

    safestr_release(resource);
    // XXX XXX LEAK. We need to retain this URL for the lifetime of the request.
    // safestr_release(url);

    if (*error != CURLE_OK)
        return NULL;

    *error = CURLE_OK;
    return conn->handle;
}
#endif

/**
 * @} S3Curl
 */

/**
 * @} S3Connection
 */
