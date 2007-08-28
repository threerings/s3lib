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
 *
 * @internal
 * @attention
 * We want to enforce strict typing externally and handle standard list tasks,
 * but we don't actually need to wrap the standard list types. Instead, we
 * just return a pointer to the lnode_t struct.
 *
 * C99, 6.2.5.26 - Types:
 * All pointers to structure types shall have the same representation and
 * alignment requirements as each other.
 */

/**
 * @defgroup S3List S3Lib List Datatype
 * @ingroup S3Library
 * @{
 */

/**
 * An S3List maintains a linked list of const char * elements.
 * @attention Operations on a S3List instance are not guaranteed thread-safe, and
 * a S3List should not be shared between threads without external synchronization.
 *
 * @internal
 * @warning This structure is a dummy. It is used only to generate
 * doxygen docs. We simply cast #list_t pointers to S3List pointers.
 */
struct S3List {
};

/**
 * A S3List element.
 *
 * @internal
 * @warning This structure is a dummy. It is used only to generate
 * doxygen docs. We simply cast #lnode_t pointers to S3ListNode pointers.
 */
struct S3ListNode {
};

/**
 * Create a new, empty S3List instance.
 * @return A newly allocated S3List, or NULL on failure
 * @sa s3list_free
 */
S3_DECLARE S3List *s3list_new () {
    list_t *list;

    /* Allocate a new, empty list */    
    list = list_create(LISTCOUNT_T_MAX);
    if (list == NULL)    
        return NULL;

    return (S3List *) list;
}

/**
 * Deallocate all resources associated with \a list.
 * @param list A S3List instance.
 */
S3_DECLARE void s3list_free (S3List *list) {
    list_t *klist;
    lnode_t *node;

    /* Unmask the villian's true type */
    klist = (list_t *) list;

    /* Iterate over the list and free the node data */
    node = list_first(klist);
    while (node != NULL) {
        lnode_t *next;

        /* Release the node value */
        safestr_t val = lnode_get(node);
        assert(val != NULL);
        safestr_release(val);

        /* Fetch the next node */
        next = list_next(klist, node);

        /* Delete the current node from the list */
        list_delete(klist, node);

        /* Free the current node */
        lnode_destroy(node);

        node = next;
    }
    assert(list_isempty(klist));
    list_destroy(klist);
}

/**
 * Append a string element to the list.
 * @param list S3List to modify
 * @param string The string to append to the list. The string will be copied.
 * @return true on success, or false on failure. This function should not fail unless available memory has been exhausted.
 */
S3_DECLARE bool s3list_append (S3List *list, const char *string) {
    list_t *klist;
    lnode_t *node;
    safestr_t data;

    /* Unmask the villian's true type */
    klist = (list_t *) list;

    data = s3_safestr_create(string, SAFESTR_IMMUTABLE);
    if (!data)
        return false;

    node = lnode_create(data);
    if (!node) {
        safestr_release(data);
        return false;
    }

    list_append(klist, node);
    return true;
}

/**
 * Returns a borrowed reference to the first list element.
 * @param list A S3List instance.
 * @return Borrowed reference to the first list element, or NULL if the list is empty.
 */
S3_DECLARE S3ListNode *s3list_first (S3List *list) {
     return (S3ListNode *) list_first((list_t *) list);
}

/**
 * If the provided node has a successor, a pointer to that successor is returned.
 * Otherwise, returns NULL.
 * @param list A S3List instance.
 * @param node A node from S3List.
 * @return Returns the next node, or NULL.
 */
S3_DECLARE S3ListNode *s3list_next (S3List *list, S3ListNode *node) {
    return (S3ListNode *) list_next((list_t *) list, (lnode_t *) node);
}

/**
 * Returns a borrowed reference to a list element's string value.
 * @param node A S3ListNode.
 * @return Borrowed reference to the list node's string value.
 */
S3_DECLARE const char *s3list_node_value (S3ListNode *node) {
    return lnode_get((lnode_t *) node);
}

/*!
 * @} S3List
 */
