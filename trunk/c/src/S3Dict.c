/*
 * S3Dict.c vi:ts=4:sw=4:expandtab:
 * Amazon S3 Library
 *
 * Author: Landon Fuller <landonf@bikemonkey.org>
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
 * @brief S3Lib Dictionary Implementation
 * @author Landon Fuller <landonf@bikemonkey.org>
 */

/**
 * @defgroup S3Dict 
 * @ingroup S3Library
 * @{
 */

static void s3dict_dealloc (S3TypeRef object);
static void s3dict_iterator_dealloc (S3TypeRef object);

/**
 * Dictionary class.
 *
 * Maintains a hash table of S3Lib objects (eg, S3String) and their associated values.
 * Dictionaries are not type-safe -- foot-shooting is entirely possible. Caveat emptor.
 */
struct S3Dict {
    S3RuntimeBase base;

    /** @internal Backing hash table. */
    hash_t *hash;
};

/** @internal S3Dict Class Definition */
static S3RuntimeClass S3DictClass = {
    .dealloc = s3dict_dealloc
};

/**
 * A S3Dict Iterator context.
 *
 * Iterates over dictionary keys.
 *
 * @sa s3header_dict_iterator_new
 * @sa s3header_dict_next
 */
struct S3DictIterator {
    S3RuntimeBase base;

    /** @internal Reference to the iterated dict. */
    S3Dict *dict;

    /** @internal The iteration context. */
    hscan_t scanner;
};

/** @internal S3DictIterator Class Definition */
static S3RuntimeClass S3DictIteratorClass = {
    .dealloc = s3dict_iterator_dealloc
};

/**
 * @internal
 * S3Dict hnode allocation function.
 *
 * This function simply allocates space for a new hnode_t.
 */
static hnode_t *s3dict_hnode_alloc(S3_UNUSED void *context) {
     return malloc(sizeof(hnode_t));
}

/**
 * @internal
 * S3Dict hnode de-allocation function.
 *
 * Deallocate the hnode, and #s3_release the retained key and value.
 */
static void s3dict_hnode_free(hnode_t *node, S3_UNUSED void *context) {
    S3TypeRef key, value;

    /* Fetch the node's key and value */
    key = (S3TypeRef) hnode_getkey(node);
    value = (S3TypeRef) hnode_get(node);

    /* No node should be inserted with a NULL key or value */
    assert(value != NULL);
    assert(key != NULL);

    /* Deallocate  */
    s3_release(key);
    s3_release(value);
    free(node);
}

/**
 * @internal
 * Kazlib-compatible hash callback function.
 */
static hash_val_t s3dict_hash_cb (const void *obj) {
    return s3_hash((S3TypeRef) obj);
}

/**
 * @internal
 * Kazlib-compatible comparison callback function.
 */
static int s3dict_equals_cb (const void *obj1, const void *obj2) {
    if (s3_equals((S3TypeRef) obj1, (S3TypeRef) obj2))
        return 0;
    else
        return -1;
}
 


/**
 * Allocate a new dictionary.
 *
 * @return Empty dictionary, or NULL on failure.
 */
S3_DECLARE S3Dict *s3dict_new () {
    S3Dict *dict;

    /* Allocate the object */
    dict = s3_object_alloc(&S3DictClass, sizeof(S3Dict));
    if (dict == NULL)
        return NULL;

    /* Allocate our hash table */
    dict->hash = hash_create(HASHCOUNT_T_MAX, s3dict_equals_cb, s3dict_hash_cb);
    if (dict->hash == NULL)
        goto error;

    /* Set our custom node allocation/deallocation callbacks */
    hash_set_allocator(dict->hash, s3dict_hnode_alloc, s3dict_hnode_free, NULL);

    return dict;

error:
    s3_release(dict);
    return NULL;
}


/**
 * Deallocate all resources assocated with @a object.
 * @param object A S3Dict instance.
 */
static void s3dict_dealloc(S3TypeRef object) {
    S3Dict *dict = (S3Dict *) object;

    assert(s3_instanceof(dict, &S3DictClass));
    if (dict->hash != NULL) {
        /* Free all nodes. */
        hash_free_nodes(dict->hash);

        /* Free the hash table. */
        hash_destroy(dict->hash);
    }
}

/**
 * Add the given key-value pair to the provided S3Dict.
 * If the key already exists in @a dict, it will be replaced.
 *
 * @param dict Dictionary to modify.
 * @param key The key for @a value.
 * @param value The value for @a key
 * @return True on success, or false on failure.
 */
S3_DECLARE bool s3dict_put (S3Dict *dict, S3TypeRef key, S3TypeRef value) {
    hnode_t *prev_node;

    /* Delete existing hash entry, if it exists */
    prev_node = hash_lookup(dict->hash, key);
    if (prev_node != NULL)
        hash_delete_free(dict->hash, prev_node);

    /* Add the key-value to the hash. */
    if (!hash_alloc_insert(dict->hash, s3_retain(key), s3_retain(value)))
        return false;

    return true;
}

/**
 * Return the value associated with the given key.
 *
 * If the key does not exist in @a dict, NULL will be return.
 *
 * @param dict Dictionary from which to fetch the value.
 * @param key The key for which to return the associated value.
 * @return An S3 object instance if the key exists, or NULL if the key can not be found.
 */

S3_DECLARE S3TypeRef s3dict_get (S3Dict *dict, S3TypeRef key) {
    hnode_t *node;

    /* Look up the key */
    node = hash_lookup(dict->hash, key);
    if (node == NULL)
        return NULL;

    /* Return the value */
    return (S3TypeRef) hnode_get(node);
}

/**
 * Returns a newly allocated S3DictIterator iteration context for the provided
 * S3Dict. The provided context can be used to iterate over all keys of the
 * S3Dict.
 *
 * @warning Any modification of the hash table during iteration is UNSAFE and WILL cause
 * undefined behavior.
 *
 * @param dict Dictionary to iterate.
 * @return A S3DictIterator instance on success, or NULL if a failure occurs.
 */
S3_DECLARE S3DictIterator *s3dict_iterator_new (S3Dict *dict) {
    S3DictIterator *iterator;

    /* Alloc and initialize our iterator */
    iterator = s3_object_alloc(&S3DictIteratorClass, sizeof(S3DictIterator));
    if (iterator == NULL)
        return NULL;

    /* Save a reference to the dictionary */
    iterator->dict = s3_retain(dict);

    /* Initialize the iterator. */
    hash_scan_begin(&iterator->scanner, dict->hash);

    return iterator;
}


/**
 * Returns the next dictionary key, if any unvisited nodes remain, or NULL.
 *
 * @param iterator An S3DictIterator instance allocated via #s3dict_iterator_new
 * @return The next dictionary key instance, or NULL if none remain. The order in which nodes are returned is undefined.
 */
S3_DECLARE S3TypeRef s3dict_iterator_next (S3DictIterator *iterator) {
    hnode_t *node;

    node = hash_scan_next(&iterator->scanner);
    if (node == NULL)
        return NULL;

    return (S3TypeRef) hnode_getkey(node);
}


/**
 * S3DictIterator deallocation callback.
 * @warning Do not call directly, use #s3_release
 * @param iterator Iterator to deallocate.
 */
static void s3dict_iterator_dealloc (S3TypeRef object) {
    S3DictIterator *iterator = (S3DictIterator *) object;

    if (iterator->dict != NULL)
        s3_release(iterator->dict);
}


/*!
 * @} S3Dict
 */
