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


#include <stdlib.h>
#include <string.h>
#include <assert.h>

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <S3Lib.h>

/**
 * @file
 * @brief S3 connection management.
 * @author Landon Fuller <landonf@threerings.net>
 */

/**
 * @defgroup S3Connection S3 Connection Management
 * @ingroup S3Library
 * @{
 */

/** Default Amazon S3 URL. */
S3_DECLARE const char S3_DEFAULT_URL[] = "https://s3.amazonaws.com";

/**
 * Maintains S3 connection state.
 * @warning S3Connection instances are not re-entrant, and must not be shared between threads.
 */
struct S3Connection {
    /** @internal
     * AWS access key id. */
    char *aws_id;

    /** @internal
     * AWS private key. */
    char *aws_key;

    /** @internal
     * S3 server url. */
    char *s3_url;

    /** @internal
     * Cached cURL handle (not thread-safe). */
    CURL *handle;
};

/**
 * Instantiate a new #S3Connection instance.
 * @attention Instances of #S3Connection are not re-entrant, and should not be
 * shared between multiple threads.
 *
 * @param aws_id Your Amazon AWS Id.
 * @param aws_key Your Amazon AWS Secret Key.
 * @return A new #S3Connection instance, or NULL on failure.
 */
S3_DECLARE S3Connection *s3connection_new (const char *aws_id, const char *aws_key) {
    S3Connection *conn;

    /* Allocate a new S3 Connection. */
    conn = calloc(1, sizeof(S3Connection));
    if (conn == NULL)
        return NULL;

    /* Access id */
    conn->aws_id = strdup(aws_id);
    if (conn->aws_id == NULL)
        goto error;    

    /* Access key */
    conn->aws_key = strdup(aws_key);
    if (conn->aws_key == NULL)
        goto error;

    /* Default S3 URL */
    conn->s3_url = strdup(S3_DEFAULT_URL);
    if (conn->s3_url == NULL)
        goto error;

    /* cURL handle */
    conn->handle = curl_easy_init();
    if (conn->handle == NULL)
        goto error;

    return (conn);    

error:
    s3connection_free(conn);
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
S3_DECLARE bool s3connection_set_url (S3Connection *conn, const char *s3_url) {
    /* Free the old URL. */
    if (conn->s3_url != NULL)
        free(conn->s3_url);

    /* Copy the new URL */
    conn->s3_url = strdup(s3_url);

    /* Success */
    return true;
}

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

/**
 * Create a new S3 bucket
 *
 * @param conn A valid S3Connection instance.
 * @param bucketName The name of the bucket to create.
 * @return A #s3error_t result.
 */
S3_DECLARE void *s3connection_create_bucket (S3Connection *conn, const char *bucketName) {
    CURLcode error;
    
    if (s3curl_create_bucket(conn, bucketName, &error) == NULL)
        return NULL;

    error = curl_easy_perform(conn->handle);
    if (error != CURLE_OK) {
        fprintf(stderr, "Failure: %s\n", curl_easy_strerror(error));
    } else {
        fprintf(stderr, "Success");        
    }

    return NULL;
}

/**
 * Close and free a #S3Connection instance.
 * @param conn A #S3Connection instance.
 */
S3_DECLARE void s3connection_free (S3Connection *conn) {
    if (conn->aws_id != NULL)
        free(conn->aws_id);

    if (conn->aws_key != NULL)
        free(conn->aws_key);

    if (conn->s3_url != NULL)
        free(conn->s3_url);

    if (conn->handle != NULL) 
        curl_easy_cleanup(conn->handle);

    free(conn);
}

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
S3_DECLARE CURL *s3curl_create_bucket (S3Connection *conn, const char *bucketName, CURLcode *error) {
    char *url = NULL;
    char *resource;

    /* Reset the handle */
    curl_easy_reset(conn->handle);
    
    s3connection_reset_curl(conn);

    /* Set the request type */
    if ((*error = curl_easy_setopt(conn->handle, CURLOPT_UPLOAD, 1)) != CURLE_OK)
        return NULL;

    /* Create and set the resource URL. */
    resource = curl_easy_escape(conn->handle, bucketName, 0);
    
    // s3_url + "/" + resource + \0 terminator
    url = malloc(strlen(conn->s3_url) + 1 + strlen(resource) + 1);
    strcpy(url, conn->s3_url);
    strcat(url, "/");
    strcat(url, resource);

    DEBUG("Configuring handle with URL: %s", url);

    *error = curl_easy_setopt(conn->handle, CURLOPT_URL, url);
    curl_free(resource);
    // XXX leak. Curl requires that we own, and keep valid, any data we pass to it
    // for the length of the option's lifetime. Need to replace this with a non-curl
    // specific S3Request context.
    // free(url);

    if (*error != CURLE_OK)
        return NULL;

    *error = CURLE_OK;
    return conn->handle;
}

/**
 * @} S3Curl
 */

/**
 * @} S3Connection
 */