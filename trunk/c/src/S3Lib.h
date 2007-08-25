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

#include <config.h>

#include <stdbool.h>

/* Win32-compatible 'extern' */
#if defined(__WIN32__)
    #if defined(TR_BUILDING_s3lib_LIB) // Building s3lib
    #define TR_EXTERN  __declspec(dllexport)
    #define TR_DECLARE __declspec(dllexport)
    #else // Not building s3lib
    #define TR_EXTERN __declspec(dllimport) extern
    #define TR_DECLARE __declspec(dllimport) extern
    #endif /* TR_BUILDING_s3lib_LIB */
#else /* __WIN32__ */
    #define TR_EXTERN extern
    #define TR_DECLARE
#endif /* !__WIN32__ */


/* cURL includes */
#include <curl/curl.h>

/*!
 * @defgroup S3Library Amazon S3 Library
 * @{
 */

/* s3lib includes */
#include "S3Error.h"
#include "S3Connection.h"

/* s3lib functions */
TR_EXTERN void s3lib_global_init (void);

/* private s3lib functions */
TR_DECLARE void s3lib_enable_debugging (bool flag);
TR_PRIVATE bool s3lib_debugging ();

#ifdef TR_BUILDING_s3lib_LIB
#define DEBUG(msg, args...) \
    if (s3lib_debugging()) \
        fprintf(stderr, "[%s in %s:%d] " msg "\n", __func__, __FILE__, __LINE__, ## args)
#endif

/*!
 * @} S3Lib
 */

#endif /* S3LIB_H */
