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

#include <string.h>
#include <stdlib.h>
#include <assert.h>

#include "S3Lib.h"

/**
 * @file
 * @brief S3Lib List Implementation.
 * @author Landon Fuller <landonf@threerings.net>
 */

/**
 * @defgroup S3List Linked List
 * @ingroup S3DataStructures
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
    .version = S3_CURRENT_RUNTIME_CLASS_VERSION,
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
    .version = S3_CURRENT_RUNTIME_CLASS_VERSION,
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
 * S3List deallocation callback.
 * @warning Do not call directly, use #s3_release
 *
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
        S3TypeRef val = lnode_get(node);
        assert(val != NULL);
        s3_release(val);

        /* Fetch the next node */
        next = list_next(&list->ctx, node);

        /* Delete the current node from the list */
        list_delete(&list->ctx, node);

        /* Free the current node */
        lnode_destroy(node);

        node = next;
    }
    assert(list_isempty(&list->ctx));
}

/**
 * Create a shallow copy of a list.
 * Referenced objects will not be copied.
 *
 * @param list A S3List instance to copy.
 * @return Returns a newly allocated copy of @a list, or NULL if a failure occured.
 * This function should not fail unless available memory has been exhausted.
 * @attention It is the caller's responsibility to free the returned list.
 */
S3_DECLARE S3List *s3list_copy (S3List *list) {
    S3List *copy;
    list_t *source;
    lnode_t *node;

    source = &list->ctx;
    
    /* Allocate a new list. */
    copy = s3list_new();
    if (copy == NULL)
        return NULL;

    /* Iterate over the source list, appending the data to the copy */
    node = list_first(source);
    while (node != NULL) {
        lnode_t *next;

        /* Get the node value, add it to the new list */
        S3TypeRef val = lnode_get(node);

        if (!s3list_append(copy, val))
            goto error;

        /* Fetch the next node */
        next = list_next(source, node);
        node = next;
    }

    return copy;

error:
    s3_release(copy);
    return NULL;
}

/**
 * Append a new S3 object to the list.
 * @param list S3List to modify
 * @param object The object to append to the list. This MUST be an S3 object type.
 * @return true on success, or false on failure. This function should not fail unless available memory has been exhausted.
 */
S3_DECLARE bool s3list_append (S3List *list, S3TypeRef object) {
    lnode_t *node;

    /* Allocate a new node with an object reference */
    node = lnode_create(s3_retain(object));
    if (!node) {
        s3_release(object);
        return false;
    }

    /* Append the node */
    list_append(&list->ctx, node);
    return true;
}

/**
 * @internal
 * An implementation of a ASCII lexicographical #S3String comparison function for use with #s3list_sort.
 */
S3_PRIVATE int s3list_lexicographical_compare (S3TypeRef elem1, S3TypeRef elem2, S3_UNUSED const void *context) {
    return strcmp(s3string_cstring(elem1), s3string_cstring(elem2));
}

/**
 * @internal
 * Maintains the #s3list_compare_t comparison function for use by the
 * #list_compare implementation.
 */
struct ListSortContext {
    s3list_compare_t func;
    const void *context;
};

/**
 * @internal
 * A comparison function compatible with our backing list's sort implementation. Simply calls out to the
 * real s3list_compare_t implementation.
 */
static int list_compare (const void *entry1, const void *entry2, const void *context) {
    const struct ListSortContext *sorter = context;
    
    return sorter->func((S3TypeRef)entry1, (S3TypeRef)entry2, sorter->context);
}

/**
 * Sort the list, using the provided comparison function.
 * @param list S3List to sort.
 * @param func Comparison function.
 * @param context Context passed to comparison function.
 */
S3_DECLARE void s3list_sort (S3List *list, s3list_compare_t func, const void *context) {
    struct ListSortContext sorter = {
        .func = func,
        .context = context
    };

    list_sort(&list->ctx, &list_compare, &sorter);
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
 * S3ListIterator deallocation callback.
 * @warning Do not call directly, use #s3_release
 *
 * @param iterator Iterator to deallocate.
 */
static void s3list_iterator_dealloc (S3TypeRef obj) {
    S3ListIterator *iterator = (S3ListIterator *) obj;

    if (iterator->list != NULL)
        s3_release(iterator->list);
}


/**
 * Returns a borrowed reference to the next list value, if any unvisited nodes remain, or NULL.
 *
 * @param iterator An S3ListIterator instance.
 * @return The next list value, or NULL if none remain.
 */
S3_DECLARE S3TypeRef s3list_iterator_next (S3ListIterator *iterator) {
    S3TypeRef value;

    /* If node is NULL, end of list */
    if (iterator->current == NULL)
        return NULL;

    /* Otherwise, save the return value and advance the node */
    value = lnode_get(iterator->current);
    iterator->current = list_next(&(iterator->list->ctx), iterator->current);

    return value;
}

/**
 * Return true if any unvisited list nodes remain, or false if the end
 * of the list has been released.
 *
 * @param iterator An S3ListIterator instance.
 * @return true if any unvisited list nodes remain.
 */
S3_DECLARE bool s3list_iterator_hasnext (S3ListIterator *iterator) {
    /* If node is NULL, end of list */
    if (iterator->current == NULL)
        return false;

    return true;
}

/*!
 * @} S3List
 */
