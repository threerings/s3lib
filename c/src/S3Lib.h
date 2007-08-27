/*
 * S3Lib.h vi:ts=4:sw=4:expandtab:
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

#ifndef S3LIB_H
#define S3LIB_H

#include <stdbool.h>

/*
 * Declaration visibility scoping
 */
#if defined(__WIN32__)
    #if defined(TR_BUILDING_s3lib_LIB)
        // Building s3lib
        #define S3_EXTERN  __declspec(dllexport)
        #define S3_DECLARE __declspec(dllexport)
    #else
        // Not building s3lib
        #define S3_EXTERN __declspec(dllimport) extern
        #define S3_DECLARE __declspec(dllimport) extern
    #endif /* TR_BUILDING_s3lib_LIB */
#else /* __WIN32__ */
    #if defined(TR_BUILDING_s3lib_LIB) && defined(GCC_VISIBILITY_SUPPORT)
        #define S3_EXTERN extern __attribute__ ((visibility("default")))
        #define S3_DECLARE __attribute__ ((visibility("default")))
        // We default to hidden, but it doesn't hurt to be explicit
        #define S3_PRIVATE __attribute__ ((visibility("hidden")))
    #else
        #define S3_EXTERN extern
        #define S3_DECLARE
        #define S3_PRIVATE
    #endif
#endif /* !__WIN32__ */

/*
 * S3Lib private API
 */
#ifdef TR_BUILDING_s3lib_LIB
    #define S3LIB_PRIVATE_API
    #include "S3LibPrivate.h"
#endif

/*
 * S3lib includes
 */
#include "S3List.h"
#include "S3Error.h"
#include "S3Connection.h"
#include "S3Header.h"
#include "S3Request.h"

/*
 * S3lib functions
 */
S3_EXTERN void s3lib_global_init (void);
S3_EXTERN void s3lib_enable_debugging (bool flag);

#endif /* S3LIB_H */
