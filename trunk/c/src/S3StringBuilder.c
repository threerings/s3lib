/*
 * S3StringBuilderBuilder.c vi:ts=4:sw=4:expandtab:
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
#include <string.h>
#include <assert.h>

#include "S3Lib.h"

/**
 * @file
 * @brief S3Lib String Implementation.
 * @author Landon Fuller <landonf@threerings.net>
 */

/**
 * @defgroup S3StringBuilder Mutable string.
 * @ingroup S3DataStructures
 * @{
 */

static void s3stringbuilder_dealloc (S3TypeRef obj);
static long s3stringbuilder_hash (S3TypeRef obj);
static bool s3stringbuilder_equals (S3TypeRef self, S3TypeRef other);

/**
 * An S3StringBuilder wraps a mutable string buffer, implementing effecient,
 * incremental building of a string buffer.
 */
struct S3StringBuilder {
    S3RuntimeBase base;

    /** @internal Wrapped safestr */
    safestr_t data;
};


/** @internal S3StringBuilder Class Definition */
static S3RuntimeClass S3StringBuilderClass = {
    .hash = s3stringbuilder_hash,
    .equals = s3stringbuilder_equals,
    .dealloc = s3stringbuilder_dealloc
};

/**
 * Create a new, empty S3StringBuilder instance, with an initial
 * capacity specified by the @a capacity argument.
 * 
 * @return A newly allocated S3StringBuilder, or NULL on failure
 */
S3_DECLARE S3StringBuilder *s3stringbuilder_new (uint32_t capacity) {
    S3StringBuilder *builder;
    
    /* Allocate */
    builder = s3_object_alloc(&S3StringBuilderClass, sizeof(S3StringBuilder));
    if (builder == NULL)
        return NULL;

    /* Initialize */
    builder->data = safestr_alloc(capacity, 0);

    return builder;
}

/**
 * Return a string representing the data in this string buffer.
 *
 * @param builder The S3StringBuilder instance.
 * @return A S3String instance containing the data in this string buffer.
 */
S3_DECLARE S3String *s3stringbuilder_string (S3StringBuilder *builder) {
    return S3STR(s3_safestr_char(builder->data));
}

/**
 * Append the source string to the given string builder.
 *
 * @param builder Builder to which the string will be appended.
 * @param append String to append.
 */
S3_DECLARE void s3stringbuilder_append (S3StringBuilder *builder, S3String *append) {
    safestr_append(&builder->data, s3string_safestr(append));
}

/**
 * Return the string buffer's length in bytes.
 *
 * @param builder The #S3StringBuilder instance.
 */
S3_DECLARE size_t s3stringbuilder_length (S3StringBuilder *builder) {
    /* safestr returns a uint32_t, but we don't want to limit our public API to less than the platform's size_t. */
    assert(sizeof(uint32_t) >= sizeof(size_t));

    return safestr_length(builder->data);
}

/**
 * @internal
 *
 * S3StringBuilder deallocation callback.
 * @warning Do not call directly, use #s3_release
 *
 * @param obj A #S3StringBuilder instance.
 */
static void s3stringbuilder_dealloc (S3TypeRef obj) {
    S3StringBuilder *builder;

    builder = (S3StringBuilder *) obj;

    /* Free any data */
    safestr_release(builder->data);
}


/**
 * @internal
 *
 * S3StringBuilder hash callback.
 *
 * Hash algorithm borrowed from kazlib.
 * @warning Do not call directly, use #s3_hash
 *
 * @param obj A S3StringBuilder instance.
 */
static long s3stringbuilder_hash (S3TypeRef obj) {
    S3StringBuilder *builder = (S3StringBuilder *) obj;
    return s3cstring_hash(s3_safestr_char(builder->data));
}

/**
 * @internal
 *
 * S3StringBuilder equality callback.
 * @warning Do not call directly, use #s3_hash
 *
 * @param self A S3StringBuilder instance.
 * @param other Object to compare against
 */

static bool s3stringbuilder_equals (S3TypeRef self, S3TypeRef other) {
    /* If it's not a string, it can't be equal */
    if (!s3_instanceof(other, &S3StringBuilderClass))
        return false;

    /* Cast to string builders. */
    S3StringBuilder *str1 = (S3StringBuilder *) self;
    S3StringBuilder *str2 = (S3StringBuilder *) other;

    /* Do the comparison */
    if (safestr_compare(str1->data, str2->data, 0) != 0)
        return false;
    else
        return true;
}

/*!
 * @} S3StringBuilder
 */
