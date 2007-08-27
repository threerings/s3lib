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
 * @brief S3Lib list implementation.
 * @author Landon Fuller <landonf@threerings.net>
 */

/**
 * @defgroup S3List S3Lib list handling
 * @ingroup S3Library
 * @{
 */

/**
 * An S3List maintains a linked list of const char * elements.
 * @attention Operations on a S3List instance are not guaranteed thread-safe, and
 * a S3List should not be shared between threads without external synchronization.
 */
struct S3List {
    list_t ctx;
};

/**
 * Create a new, empty S3List instance.
 * @return A newly allocated S3List, or NULL on failure
 */
S3_DECLARE S3List *s3list_new () {
    S3List *list = malloc(sizeof(S3List));
    if (list == NULL)
        return NULL;

    /* Initialize a new, empty list */
    list_init(&list->ctx, LISTCOUNT_T_MAX);

    return list;
}

/**
 * Deallocate all resources associated with \a list.
 * @param list A S3List instance.
 */
S3_DECLARE void s3list_free (S3List *list) {
    lnode_t *node;

    /* Iterate over the list and free the node data */
    node = list_first(&list->ctx);
    while (node != NULL) {
        lnode_t *next;

        /* Release the node value */
        safestr_t val = lnode_get(node);
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

/*!
 * @} S3List
 */
