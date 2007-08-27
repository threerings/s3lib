/*
 * S3Header.c vi:ts=4:sw=4:expandtab:
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
 * @brief S3 HTTP header handling.
 * @author Landon Fuller <landonf@threerings.net>
 */

/**
 * @defgroup S3Header S3 HTTP Header Handling Functions
 * @ingroup S3Library
 * @{
 */

/**
 * S3 HTTP Request/Response Headers
 *
 * Maintains a hash table of HTTP headers and their associated values. 
 */
struct S3HeaderDictionary {
    /** @internal A hash table of S3Header instances, keyed by case-sensitive header name */
     hash_t *hash;
};


/**
 * S3Header
 *
 * HTTP header and its associated value(s).
 */
struct S3Header {
    /** @internal The header name */
    safestr_t name;

    /** @internal The header value(s) */
    list_t *values;
};


/**
 * Allocate a new S3 Header instance.
 * @param name HTTP header name
 * @return S3 header, or NULL on failure.
 * @sa s3header_append_value
 */
S3_DECLARE S3Header *s3header_new (const char *name) {
    S3Header *header;

    /* Allocate our empty header */
    header = calloc(1, sizeof(S3Header));
    if (header == NULL)
        return NULL;

    /* The header name */
    header->name = s3_safestr_create(name, SAFESTR_IMMUTABLE);

    /* An empty list for header value(s) */
    header->values = list_create(-1);
    if (header->values == NULL)
        goto error;

    /* All done */
    return header;

error:
    s3header_free(header);
    return NULL;
}


/**
 * Deallocate all resources associated with the S3Header.
 * @param header A S3Header instance
 */
S3_DECLARE void s3header_free (S3Header *header) {
    if (header->name != NULL)
        safestr_release(header->name);

    if (header->values != NULL) {
        list_destroy_nodes(header->values);
        list_destroy(header->values);
    }
    free(header);
}


/**
 * @internal
 * S3Header hnode allocation function. Allocates a new key -> S3Header hash node
 */
static hnode_t *s3header_hnode_alloc(S3_UNUSED void *context) {
     return malloc(sizeof(hnode_t));
}


/**
 * @internal
 * S3Header hnode de-allocation function.
 */
static void s3header_hnode_free(hnode_t *node, S3_UNUSED void *context) {
    S3Header *value;
    safestr_t key;

    /* Fetch the node's key and value */
    value = (S3Header *) hnode_get(node);
    key = (safestr_t) hnode_getkey(node);

    /* No node should be inserted with a NULL key or value */
    assert(value != NULL);
    assert(key != NULL);

    /* Deallocate  */
    s3header_free(value);
    safestr_release(key);
    free(node);
}


/**
 * Allocate a new HTTP header dictionary.
 * @return Empty header dictionary, or NULL on failure.
 */
S3_DECLARE S3HeaderDictionary *s3header_dict_new () {
    S3HeaderDictionary *headers;

    /* Allocate an empty struct */
    headers = calloc(1, sizeof(S3HeaderDictionary));
    if (headers == NULL)
        return NULL;

    /* Allocate our hash table */
    headers->hash = hash_create(HASHCOUNT_T_MAX, NULL, NULL);
    if (headers->hash == NULL)
        goto error;

    /* Set our custom node allocation/deallocation callbacks */
    hash_set_allocator(headers->hash, s3header_hnode_alloc, s3header_hnode_free, NULL);

    return headers;

error:
    s3header_dict_free(headers);
    return NULL;
}


/**
 * Deallocate all resources assocated with @a headers.
 * @param headers A S3HeaderDictionary instance.
 */
S3_DECLARE void s3header_dict_free(S3HeaderDictionary *headers) {
    if (headers->hash != NULL) {
        hash_free_nodes(headers->hash);
        hash_destroy(headers->hash);        
    }
    free(headers);
}

/*!
 * @} S3Header
 */
