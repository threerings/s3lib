/*
 * S3LibPrivate.h vi:ts=4:sw=4:expandtab:
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

#ifndef S3LIBPRIVATE_H
#define S3LIBPRIVATE_H

#include <safestr.h>
#include <curl/curl.h>

#include "hash.h"
#include "list.h"

/**
 * @file
 * @brief S3Lib Core Private Implementation
 * @author Landon Fuller <landonf@threerings.net>
 */

/*!
 * @addtogroup S3Library
 * @{
 */

/**
 * @internal
 * S3RuntimeClass Version. Used to ensure ABI compatibility
 * as new fields are added to the S3RuntimeClass structure.
 */
typedef enum {
    /** Invalid (eg, unset) class structure version. */
    s3runtime_class_v_invalid = 0,

    /** First version of the class structure. */
    s3runtime_class_v1 = 1
} S3RuntimeClassVersion;

/**
 * @internal
 * Current S3RuntimeClassVersion version. This must be set
 * in all S3RuntimeClass declarations.
 */
#define S3_CURRENT_RUNTIME_CLASS_VERSION s3runtime_class_v1

/**
 * @internal
 * Object instance deallocator.
 */
typedef void (*s3_dealloc_function) (S3TypeRef object);

/**
 * @internal
 * Returns an unsigned integer that may be used as a table address
 * in a hash table structure.
 *
 * The value returned by this method must not change while the object is part
 * of a collection that uses hash values to determine collection position.
 *
 * Two equal objects must hae the same hash.
 */
typedef long (*s3_hash_function) (S3TypeRef object);

/**
 * @internal
 * Object equality.
 *
 * @param self The object responsible for the comparison
 * @param other The object instance to be compared against.
 */
typedef bool (*s3_equals_function) (S3TypeRef self, S3TypeRef other);

/**
 * @internal
 * S3 object class definition. Used to implement standard, polymorphic
 * operations on S3 objects.
 */
typedef struct S3RuntimeClass {
    /** Class version declaration. */
    S3RuntimeClassVersion version;
    
    /** Deallocation function. */
    s3_dealloc_function dealloc;

    /** Hash function. */
    s3_hash_function hash;

    /** Equals function. */
    s3_equals_function equals;
} S3RuntimeClass;


/**
 * @internal
 * S3 object base class. All S3 objects start with this structure.
 * @warning Never reference any of these elements directly.
 */
typedef struct S3RuntimeBase {
    /** Object magic number. Used to verify that the given
        pointer is a valid s3 object instance. */
    uint16_t magic;
    
    /** Object reference count */
    uint32_t refCount;

    /** Object class */
    S3RuntimeClass *class;

    /** Reserved for future growth */
    void *reserved[4];
} S3RuntimeBase;


/**
 * @internal
 * Create a new safestr from a const char * string. The safestr_create function
 * does not modify the string argument, so we do the cast once here.
 */
static inline safestr_t s3_safestr_create (const char *string, u_int32_t flags) {
    return safestr_create((char *)string, flags);
}


/**
 * @internal
 * Returns a borrowed reference to the safestr's backing character array.
 */
static inline const char *s3_safestr_char (safestr_t string) {
    /* Safe strings are C strings, with meta-data stored above the char * pointer */
    return (const char *)string;
}

/* Library-private functions */
S3_PRIVATE bool s3lib_debugging ();
S3_PRIVATE bool s3_instanceof (S3TypeRef object, S3RuntimeClass *class);

#define DEBUG(msg, args...) \
    if (s3lib_debugging()) \
        fprintf(stderr, "[%s in %s:%d] " msg "\n", __func__, __FILE__, __LINE__, ## args)

/* Max & Min */
#ifndef MAX
#define MAX(a,b) (((a) > (b)) ? (a) : (b))
#endif

#ifndef MIN
#define MIN(a,b) (((a) < (b)) ? (a) : (b))
#endif

/*!
 * @} S3Library
 */

#endif /* S3LIBPRIVATE_H */
