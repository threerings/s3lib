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
 * Object instance deallocator.
 */
typedef void (*s3_dealloc_function) (S3TypeRef);

/**
 * @internal
 * S3 object class definition. Used to implement standard, polymorphic
 * operations on S3 objects.
 */
typedef struct S3RuntimeClass {
    /** Deallocation function */
    s3_dealloc_function dealloc;
} S3RuntimeClass;


/**
 * @internal
 * S3 object base class. All S3 objects start with this structure.
 * @warning Never reference any of these elements directly.
 */
typedef struct S3RuntimeBase {
    /** Object reference count */
    uint32_t refCount;

    /** Object class */
    S3RuntimeClass *class;
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


S3_PRIVATE bool s3lib_debugging ();


#define DEBUG(msg, args...) \
    if (s3lib_debugging()) \
        fprintf(stderr, "[%s in %s:%d] " msg "\n", __func__, __FILE__, __LINE__, ## args)


/*!
 * @} S3Library
 */

#endif /* S3LIBPRIVATE_H */
