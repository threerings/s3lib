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
 * @defgroup S3Header HTTP Headers
 * @ingroup S3Library
 * @{
 */

static void s3header_dealloc (S3TypeRef object);
static void s3header_dict_dealloc(S3TypeRef object);
static void s3header_dict_iterator_dealloc (S3TypeRef object);

/**
 * S3 HTTP Request/Response Headers
 *
 * Maintains a hash table of HTTP headers and their associated values. 
 */
struct S3HeaderDict {
    S3RuntimeBase base;

    /** @internal A hash table of S3Header instances, keyed by case-sensitive header name */
    hash_t *hash;
};

/** @internal S3HeaderDict Class Definition */
static S3RuntimeClass S3HeaderDictClass = {
    .dealloc = s3header_dict_dealloc
};

/**
 * A S3HeaderDict Iterator context.
 *
 * @sa s3header_dict_iterator_new
 * @sa s3header_dict_next
 */
struct S3HeaderDictIterator {
    S3RuntimeBase base;

    /** @internal Reference to the iterated dict. */
    S3HeaderDict *dict;

    /** @internal The iteration context. */
    hscan_t scanner;
};

/** @internal S3HeaderDictIterator Class Definition */
static S3RuntimeClass S3HeaderDictIteratorClass = {
    .dealloc = s3header_dict_iterator_dealloc
};

/**
 * S3Header
 *
 * HTTP header and its associated value(s).
 */
struct S3Header {
    S3RuntimeBase base;

    /** @internal The header name */
    safestr_t name;

    /** @internal The header value(s) */
    S3List *values;
};

/** @internal S3Header Class Definition */
static S3RuntimeClass S3HeaderClass = {
    .dealloc = s3header_dealloc
};

/**
 * Allocate a new S3 Header instance.
 * @param name HTTP header name
 * @param value A single HTTP header value.
 * @return S3 header, or NULL on failure.
 * @sa s3header_append_value
 */
S3_DECLARE S3Header *s3header_new (const char *name, const char *value) {
    S3Header *header;

    /* Allocate our empty header */
    header = s3_object_alloc(&S3HeaderClass, sizeof(S3Header));
    if (header == NULL)
        return NULL;

    /* The header name */
    header->name = s3_safestr_create(name, SAFESTR_IMMUTABLE);

    /* An new list for header value(s) */
    header->values = s3list_new();
    if (header->values == NULL)
        goto error;

    /* The first header value */
    if (!s3list_append(header->values, value))
        goto error;

    /* All done */
    return header;

error:
    s3_release(header);
    return NULL;
}


/**
 * #S3Header deallocation callback.
 * @warning Do not call directly, use #s3_release
 * @param header A S3Header instance
 */
static void s3header_dealloc (S3TypeRef object) {
    S3Header *header = (S3Header *) object;

    if (header->name != NULL)
        safestr_release(header->name);

    if (header->values != NULL)
        s3_release(header->values);

    free(header);
}


/**
 * Returns a borrowed reference to the header value list.
 *
 * @param header A S3Header instance.
 * @return Borrowed reference to the header value list.
 */
S3_DECLARE S3List *s3header_values (S3Header *header) {
    return header->values;
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
    s3_release(value);
    safestr_release(key);
    free(node);
}


/**
 * Allocate a new HTTP header dictionary.
 * @return Empty header dictionary, or NULL on failure.
 */
S3_DECLARE S3HeaderDict *s3header_dict_new () {
    S3HeaderDict *headers;

    /* Allocate an empty struct */
    headers = s3_object_alloc(&S3HeaderDictClass, sizeof(S3HeaderDict));
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
    s3_release(headers);
    return NULL;
}


/**
 * Deallocate all resources assocated with @a headers.
 * @param headers A S3HeaderDict instance.
 */
static void s3header_dict_dealloc(S3TypeRef object) {
    S3HeaderDict *headers = (S3HeaderDict *) object;

    if (headers->hash != NULL) {
        /* Free all nodes. */
        hash_free_nodes(headers->hash);

        /* Free the hash table. */
        hash_destroy(headers->hash);
    }

    free(headers);
}

/**
 * Add a new header value to the S3HeaderDict. If the value already exists
 * in @a headers, it will be replaced.
 *
 * @param headers Dictionary to modify.
 * @param name HTTP header name.
 * @param value HTTP header value.
 * @return True on success, or false on failure.
 */
S3_DECLARE bool s3header_dict_put (S3HeaderDict *headers, const char *name, const char *value) {
    S3Header *header = NULL;
    hnode_t *node = NULL;
    hnode_t *prev_node;

    /* Create a new header for the given name/value. */
    header = s3header_new(name, value);
    if (header == NULL)
        return false;

    /* Node to hold the header. */
    node = hnode_create(header);
    if (node == NULL)
        goto error;

    /* Delete existing hash entry, if it exists */
    prev_node = hash_lookup(headers->hash, name);
    if (prev_node != NULL)
        hash_delete(headers->hash, prev_node);

    /* Add the header to the hash, re-using our existing key reference */
    hash_insert(headers->hash, node, safestr_reference(header->name));

    return true;

error:
    if (header != NULL)
        s3_release(header);

    /* Don't need to free the header, since we've
     * already done that above. Just destroy the node. */
    if (node != NULL)
        hnode_destroy(node);

    return false;
}


/**
 * Returns a newly allocated S3HeaderDictIterator iteration context for the provided
 * S3HeaderDict. The provided context can be used to iterate over all entries of the
 * S3HeaderDict.
 *
 * @warning Any modifications of the hash table during iteration are UNSAFE and WILL cause
 * undefined behavior.
 *
 * @param headers Dictionary to iterate.
 * @return A S3HeaderDictIterator instance on success, or NULL if a failure occurs.
 */
S3_DECLARE S3HeaderDictIterator *s3header_dict_iterator_new (S3HeaderDict *headers) {
    S3HeaderDictIterator *iterator;

    /* Alloc and initialize our iterator */
    iterator = s3_object_alloc(&S3HeaderDictIteratorClass, sizeof(S3HeaderDictIterator));
    if (iterator == NULL)
        return NULL;

    /* Save a reference to the dictionary */
    s3_retain(headers);
    iterator->dict = headers;

    /* Initialize the iterator. */
    hash_scan_begin(&iterator->scanner, headers->hash);

    return iterator;
}


/**
 * Returns the next S3Header item, if any unvisited nodes remain, or NULL.
 *
 * @param iterator An S3HeaderDictIterator instance allocated via s3header_dict_iterator_new
 * @return The next S3Header instance, or NULL if none remain. The order in which nodes are returned is undefined.
 */
S3_DECLARE S3Header *s3header_dict_next (S3HeaderDictIterator *iterator) {
    hnode_t *node;

    node = hash_scan_next(&iterator->scanner);
    if (node == NULL)
        return NULL;

    return (S3Header *) hnode_get(node);
}


/**
 * S3HeaderDictIterator deallocation callback.
 * @warning Do not call directly, use #s3_release
 * @param iterator Iterator to deallocate.
 */
static void s3header_dict_iterator_dealloc (S3TypeRef object) {
    S3HeaderDictIterator *iterator = (S3HeaderDictIterator *) object;

    if (iterator->dict != NULL)
        s3_release(iterator->dict);

    free(iterator);
}


/*!
 * @} S3Header
 */
