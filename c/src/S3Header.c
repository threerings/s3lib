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
    S3String *string = s3string_new(value);

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

    /* The first header value. TODO: Accept S3String values instead */
    if (!s3list_append(header->values, string))
        goto error;
    s3_release(string);

    /* All done */
    return header;

error:
    s3_release(string);
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

/*!
 * @} S3Header
 */
