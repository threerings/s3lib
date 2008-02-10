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
#include <string.h>
#include <assert.h>

#include "S3Lib.h"

/**
 * @file
 * @brief S3Lib String Implementation.
 * @author Landon Fuller <landonf@threerings.net>
 */

/**
 * @defgroup S3String Simple string
 * @ingroup S3DataStructures
 * @{
 */

static void s3string_dealloc (S3TypeRef obj);
static long s3string_hash (S3TypeRef obj);
static bool s3string_equals (S3TypeRef self, S3TypeRef other);

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
    .hash = s3string_hash,
    .equals = s3string_equals,
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
 * Create a new S3String instance, using the provided printf-style format string and arguments.
 * The returned string will be autoreleased, and you do not need to free it.
 *
 * @param format printf-style format string.
 * @return An auto-released S3String instance.
 */
S3_DECLARE S3String *s3string_withformat (const char *format, ...) {
    va_list ap;
    char *output;
    S3String *ret;

    va_start(ap, format);
    vasprintf(&output, format, ap);
    va_end(ap);

    assert(output != NULL);
    ret = s3string_new(output);
    free(output);

    return s3_autorelease(ret);
}

/**
 * Returns true if the S3String instance starts with the given
 * string.
 *
 * @param string The string to be checked.
 * @param substring The substring to look for.
 * @return True if the string starts with substring, false others.
 */
S3_DECLARE bool s3string_startswith (S3String *string, S3String *substring) {
    return safestr_startswith(s3string_safestr(string), s3string_safestr(substring));
}

/**
 * Returns a lowercase representation of the given string.
 *
 * @param string The source string to be converted.
 * @return A lowercase conversion of the provided string.
 */
S3_DECLARE S3String *s3string_lowercase (S3String *string) {
    safestr_t lower;

    /* Convert the string to lowercase */
    lower = safestr_clone(s3string_safestr(string), 0);
    safestr_convert(lower, SAFESTR_CONVERT_LOWERCASE);

    /* Create the return value */
    S3String *result = s3string_new(s3_safestr_char(lower));

    /* Clean up and return */
    safestr_release(lower);
    return (s3_autorelease(result));
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
 * Return the string's length in bytes.
 *
 * @param string The string instance.
 */
S3_DECLARE size_t s3string_length (S3String *string) {
    /* safestr returns a uint32_t, but we don't want to limit our public API to less than the platform's size_t. */
    assert(sizeof(uint32_t) >= sizeof(size_t));

    return safestr_length(s3string_safestr(string));
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
}


/**
 * @internal
 *
 * S3String hash callback.
 *
 * Hash algorithm borrowed from kazlib.
 * @warning Do not call directly, use #s3_hash
 *
 * @param obj A S3String instance.
 */
static long s3string_hash (S3TypeRef obj) {
    S3String *string = (S3String *) obj;

    static unsigned long randbox[] = {
        0x49848f1bU, 0xe6255dbaU, 0x36da5bdcU, 0x47bf94e9U,
        0x8cbcce22U, 0x559fc06aU, 0xd268f536U, 0xe10af79aU,
        0xc1af4d69U, 0x1d2917b5U, 0xec4c304dU, 0x9ee5016cU,
        0x69232f74U, 0xfead7bb3U, 0xe9089ab6U, 0xf012f6aeU,
    };

    const unsigned char *str = (const unsigned char *) s3string_cstring(string);
    long acc = 0;

    while (*str) {
        acc ^= randbox[(*str + acc) & 0xf];
        acc = (acc << 1) | (acc >> 31);
        acc &= 0xffffffffU;
        acc ^= randbox[((*str++ >> 4) + acc) & 0xf];
        acc = (acc << 2) | (acc >> 30);
        acc &= 0xffffffffU;
    }

    return acc;
}

/**
 * @internal
 *
 * S3String equality callback.
 * @warning Do not call directly, use #s3_hash
 *
 * @param self A S3String instance.
 * @param other Object to compare against
 */

static bool s3string_equals (S3TypeRef self, S3TypeRef other) {
    /* If it's not a string, it can't be equal */
    if (!s3_instanceof(other, &S3StringClass))
        return false;

    /* Cast to strings. */
    S3String *str1 = (S3String *) self;
    S3String *str2 = (S3String *) other;

    /* Do the comparison */
    if (strcmp(s3string_cstring(str1), s3string_cstring(str2)) != 0)
        return false;
    else
        return true;
}

/**
 * Clone a string.
 *
 * @param string A S3String instance to copy.
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
