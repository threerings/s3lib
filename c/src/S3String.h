/*
 * S3String.h vi:ts=4:sw=4:expandtab:
 * Amazon S3 Library
 *
 * Author: Landon Fuller <landonf@threerings.net>
 *
 * Copyright (c) 2007 Landon Fuller <landonf@bikemonkey.org>
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

#ifndef S3STRING_H
#define S3STRING_H

/**
 * @file
 * @brief S3Lib string implementation.
 * @author Landon Fuller <landonf@threerings.net>
 */

/*!
 * @addtogroup S3String
 * @{
 */

typedef struct S3String S3String;

S3_EXTERN S3String *s3string_new (const char *cstring);
S3_EXTERN S3String *s3string_copy (S3String *string);
S3_EXTERN S3String *s3string_withformat (const char *format, ...);
S3_EXTERN bool s3string_startswith (S3String *string, S3String *substring);
S3_EXTERN S3String *s3string_lowercase (S3String *string);
S3_EXTERN const char *s3string_cstring (S3String *string);
S3_EXTERN size_t s3string_length (S3String *string);

/** Create a new 'constant' S3 string. The returned string will be autoreleased, and you do not need to free it. */
#define S3STR(s) ((S3String *) s3_autorelease(s3string_new(s)))

#ifdef S3LIB_PRIVATE_API
S3_PRIVATE long s3cstring_hash (const char *string);
#endif

/*!
 * @} S3String
 */

#endif /* S3STRING_H */
