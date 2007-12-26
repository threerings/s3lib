/*
 * S3String.h vi:ts=4:sw=4:expandtab:
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
 * @brief S3Lib String Implementation.
 * @author Landon Fuller <landonf@threerings.net>
 */

/**
 * @defgroup S3String Simple strings
 * @ingroup S3Library
 * @{
 */

static void s3string_dealloc (S3TypeRef obj);

/**
 * An S3String wraps a simple immutable string buffer.
 *
 * S3String instances are immutable, and allow the use
 * of strings in S3Lib container classes.
 */
struct S3String {
    S3RuntimeBase base;

    /** @internal Wrapped safestr */
    safestr_t data;
};


/** @internal S3String Class Definition */
static S3RuntimeClass S3StringClass = {
    .dealloc = s3string_dealloc
};

/**
 * Create a new S3String instance, with a copy of the provided
 * C string.
 *
 * @return A newly allocated S3String, or NULL on failure
 */
S3_DECLARE S3String *s3string_new (const char *cstring) {
    S3String *string;
    
    /* Allocate */
    string = s3_object_alloc(&S3StringClass, sizeof(S3String));
    if (string == NULL)
        return NULL;

    /* Initialize */
    string->data = s3_safestr_create(cstring, SAFESTR_IMMUTABLE);

    return string;
}

/**
 * Return a borrowed reference to the S3String's backing
 * character buffer.
 *
 * @param string The string from which the buffer will be returned.
 */
S3_DECLARE const char *s3string_cstring (S3String *string) {
    return s3_safestr_char(s3string_safestr(string));
}

/**
 * @internal
 *
 * S3String deallocation callback.
 * @warning Do not call directly, use #s3_release
 *
 * @param string A S3String instance.
 */
static void s3string_dealloc (S3TypeRef obj) {
    S3String *string;

    string = (S3String *) obj;
    
    /* Free any data */
    safestr_release(string->data);

    free(string);
}

/**
 * Clone a string.
 *
 * @param S3String A S3String instance to copy.
 * @return Returns a newly allocated copy of @a string, or NULL if a failure occured.
 * This function should not fail unless available memory has been exhausted.
 * @attention It is the caller's responsibility to free the returned string.
 */
S3_DECLARE S3String *s3string_copy (S3String *string) {
    /* Strings are immutable, so we can just make a reference */
    s3_retain(string);
    return string;
}


/**
 * @internal
 * Return the @a string's backing safestr.
 */
S3_PRIVATE safestr_t s3string_safestr (S3String *string) {
    return string->data;
}

/*!
 * @} S3String
 */
