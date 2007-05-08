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

#include <s3lib.h>

#include <string.h>

/**
 * @file
 * @brief S3 connection management.
 */

/** Default Amazon S3 URL. */
const char S3_DEFAULT_URL[] = "https://s3.amazonaws.com";

/**
 * Maintains S3 connection state.
 * @attention S3Connection instances are not thread-safe.
 */
struct S3Connection {
    /* AWS access key id. */
    char *aws_id;

    /* AWS private key. */
    char *aws_key;

    /* S3 server url. */
    char *s3_url;

    /* Cached cURL handle (not thread-safe). */
    CURL *curl;
};

/**
 * Instantiate a new S3 connection instance.
 * @attention S3Connection instances are not thread-safe, and must not be
 * shared between multiple threads.
 *
 * @param aws_id Your Amazon AWS Id.
 * @param aws_key Your Amazon AWS Secret Key.
 * @return A new S3Connection instance, or NULL on failure.
 */
S3Connection *s3connection_new (const char *aws_id, const char *aws_key) {
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
    conn->curl = curl_easy_init();
    if (conn->curl == NULL)
        goto error;

    return (conn);    

error:
    s3connection_free(conn);
    return NULL;
}

/**
 * Set the connection's S3 service URL.
 * The service URL defaults to #S3_DEFAULT_URL, and will not generally need to be changed.
 *
 * @param conn A valid S3Connection instance.
 * @param s3_url The new S3 service URL.
 * @return true on success, false on failure.
 */
bool s3connection_set_url (S3Connection *conn, const char *s3_url) {
    char *saved = conn->s3_url;

    /* Free the old URL. */
    if (conn->s3_url != NULL)
        free(conn->s3_url);

    /* Copy the new URL */
    conn->s3_url = strdup(s3_url);

    /* This is unlikely; restore the URL to keep the data structure consistent. */
    if (conn->s3_url == NULL) {
        conn->s3_url = saved;
        return false;
    }

    /* Success */
    return true;
}

/**
 * Close and free a S3 connection instance.
 * @param conn An a valid S3Connection instance.
 */
void s3connection_free (S3Connection *conn) {
    if (conn->aws_id != NULL)
        free(conn->aws_id);

    if (conn->aws_key != NULL)
        free(conn->aws_key);

    if (conn->s3_url != NULL)
        free(conn->s3_url);

    if (conn->curl != NULL) 
        curl_easy_cleanup(conn->curl);
}