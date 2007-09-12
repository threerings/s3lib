/*
 * S3List.h vi:ts=4:sw=4:expandtab:
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
#include <assert.h>

#include "S3Lib.h"

/**
 * @file
 * @brief S3Lib List Implementation.
 * @author Landon Fuller <landonf@threerings.net>
 */

/**
 * @defgroup S3List Linked Lists
 * @ingroup S3Library
 * @{
 */

static void s3list_dealloc (S3TypeRef obj);
static void s3list_iterator_dealloc (S3TypeRef obj);

/**
 * An S3List maintains a linked list of const char * elements.
 * @attention Operations on a S3List instance are not guaranteed thread-safe, and
 * a S3List should not be shared between threads without external synchronization.
 */
struct S3List {
    S3RuntimeBase base;

    /** @internal List implementation context */
    list_t ctx;
};


/** @internal S3List Class Definition */
static S3RuntimeClass S3ListClass = {
    .dealloc = s3list_dealloc
};

/**
 * A S3List Iterator context.
 *
 * @sa s3list_iterator_new
 * @sa s3list_iterator_next
 */
struct S3ListIterator {
    S3RuntimeBase base;

    /** @internal Reference to the iterated list. */
    S3List *list;

    /** @internal Reference to the current list node. */
    lnode_t *current;
};

/** @internal S3ListIterator Class Definition */
static S3RuntimeClass S3ListIteratorClass = {
    .dealloc = s3list_iterator_dealloc
};

/**
 * Create a new, empty S3List instance.
 * @return A newly allocated S3List, or NULL on failure
 */
S3_DECLARE S3List *s3list_new () {
    S3List *list;
    
    /* Allocate */
    list = s3_object_alloc(&S3ListClass, sizeof(S3List));
    if (list == NULL)
        return NULL;

    /* Initialize */
    list_init(&list->ctx, LISTCOUNT_T_MAX);

    return list;
}

/**
 * @internal
 *
 * Deallocate all resources associated with \a list.
 * @param list A S3List instance.
 */
static void s3list_dealloc (S3TypeRef obj) {
    S3List *list;
    lnode_t *node;

    list = (S3List *) obj;

    /* Iterate over the list and free the node data */
    node = list_first(&list->ctx);
    while (node != NULL) {
        lnode_t *next;

        /* Release the node value */
        safestr_t val = lnode_get(node);
        assert(val != NULL);
        safestr_release(val);

        /* Fetch the next node */
        next = list_next(&list->ctx, node);

        /* Delete the current node from the list */
        list_delete(&list->ctx, node);

        /* Free the current node */
        lnode_destroy(node);

        node = next;
    }
    assert(list_isempty(&list->ctx));
    free(list);
}

/**
 * Clone a list.
 *
 * @param list A S3List instance to clone.
 * @return Returns a newly allocated clone of @a list, or NULL if a failure occured.
 * This function should not fail unless available memory has been exhausted.
 * @attention It is the caller's responsibility to free the returned list.
 */
S3_DECLARE S3List *s3list_clone (S3List *list) {
    S3List *clone;
    list_t *source;
    lnode_t *node;

    source = &list->ctx;
    
    /* Allocate a new list. */
    clone = s3list_new();
    if (clone == NULL)
        return NULL;

    /* Iterate over the source list, appending the data to the clone */
    node = list_first(source);
    while (node != NULL) {
        lnode_t *next;

        /* Get the node value, add it to the new list */
        safestr_t val = lnode_get(node);
        
        if (!s3list_append_safestr(clone, val))
            goto error;

        /* Fetch the next node */
        next = list_next(source, node);
        node = next;
    }

    return clone;

error:
    s3_release(clone);
    return NULL;
}

/**
 * Append a string element to the list.
 * @param list S3List to modify
 * @param string The string to append to the list. The string will be copied.
 * @return true on success, or false on failure. This function should not fail unless available memory has been exhausted.
 */
S3_DECLARE bool s3list_append (S3List *list, const char *string) {
    safestr_t data;
    bool result;

    data = s3_safestr_create(string, SAFESTR_IMMUTABLE);
    if (!data)
        return false;

    /* Append the string, drop our implicit reference, and return the result. */
    result = s3list_append_safestr(list, data);
    safestr_release(data);
    return (result);
}

/**
 * @internal
 *
 * Append a reference to the safestr element to the list.
 *
 * @param list S3List to modify
 * @param string The string to append to the list. The list will retain a string reference.
 * @return true on success, or false on failure. This function should not fail unless available memory has been exhausted.
 */
S3_PRIVATE bool s3list_append_safestr (S3List *list, safestr_t string) {
    lnode_t *node;

    /* Allocate a new node with a string reference */
    node = lnode_create(safestr_reference(string));
    if (!node) {
        safestr_release(string);
        return false;
    }

    /* Append the node */
    list_append(&list->ctx, node);
    return true;
}

/**
 * Returns a newly allocated S3ListIterator iteration context for the provided
 * S3List. The provided context can be used to iterate over all entries of the
 * S3List.
 *
 * @warning Any modifications to the list during iteration are UNSAFE and WILL cause
 * undefined behavior.
 *
 * @param list List to iterate.
 * @return A S3ListIterator instance on success, or NULL if a failure occurs.
 */
S3_DECLARE S3ListIterator *s3list_iterator_new (S3List *list) {
    S3ListIterator *iterator;

    /* Allocate */
    iterator = s3_object_alloc(&S3ListIteratorClass, sizeof(S3ListIterator));
    if (iterator == NULL)
        return NULL;

    /* Initialize */
    s3_retain(list);
    iterator->list = list;
    iterator->current = list_first(&list->ctx);

    /* Return */
    return iterator;
}


/**
 * @internal
 * Deallocate all resources associated with the provided S3ListIterator context.
 * @param iterator Iterator to deallocate.
 */
static void s3list_iterator_dealloc (S3TypeRef obj) {
    S3ListIterator *iterator = (S3ListIterator *) obj;

    if (iterator->list != NULL)
        s3_release(iterator->list);

    free(iterator);
}


/*
 * Returns a borrowed reference to the next list value, if any unvisited nodes remain, or NULL.
 *
 * @param iterator An S3ListIterator instance.
 * @return The next list value, or NULL if none remain.
 */
S3_DECLARE const char *s3list_iterator_next (S3ListIterator *iterator) {
    safestr_t value;

    /* If node is NULL, end of list */
    if (iterator->current == NULL)
        return NULL;

    /* Otherwise, save the return value and advance the node */
    value = lnode_get(iterator->current);
    iterator->current = list_next(&(iterator->list->ctx), iterator->current);

    return s3_safestr_char(value);
}


/*!
 * @} S3List
 */
