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
    list_t *ctx;
};

/**
 * Create a new, empty S3List instance.
 * @return A newly allocated S3List, or NULL on failure
 */
S3_DECLARE S3List *s3list_new () {
    S3List *list = malloc(sizeof(S3List));
    if (list == NULL)
        return NULL;

    /* Create a new, empty list */
    list->ctx = list_create(LISTCOUNT_T_MAX);

    return list;
}

S3_DECLARE void s3list_free (S3List *list) {
    lnode_t *node;

    /* If the list has been allocated, iterate over it and free the node data,
     * and then destroy the list */
    if (list->ctx != NULL) {
        node = list_first(list->ctx);        
        while (node != NULL) {
            lnode_t *next;

            safestr_t val = lnode_get(node);
            safestr_release(val);

            next = list_next(list->ctx, node);
            lnode_destroy(node);

            node = next;
        }
        
        list_destroy(list->ctx);
    }

    free(list);
}

/*!
 * @} S3List
 */
