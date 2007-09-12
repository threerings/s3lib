/*
 * S3Header.h vi:ts=4:sw=4:expandtab:
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

#ifndef S3HEADER_H
#define S3HEADER_H

/**
 * @file
 * @brief S3 HTTP header handling.
 * @author Landon Fuller <landonf@threerings.net>
 */

/*!
 * @addtogroup S3Header
 * @{
 */

/* S3 Header Dictionary */
typedef struct S3HeaderDict S3HeaderDict;

/* S3 Header Dictionary Iterator */
typedef struct S3HeaderDictIterator S3HeaderDictIterator;

/* S3 Header */
typedef struct S3Header S3Header;

S3_EXTERN S3Header *s3header_new (const char *name, const char *value);
S3_EXTERN S3List *s3header_values (S3Header *header);

S3_EXTERN S3HeaderDict *s3header_dict_new ();

S3_EXTERN bool s3header_dict_put (S3HeaderDict *headers, const char *name, const char *value);

S3_EXTERN S3HeaderDictIterator *s3header_dict_iterator_new (S3HeaderDict *headers);
S3_EXTERN S3Header *s3header_dict_next (S3HeaderDictIterator *iterator);

/*!
 * @} S3Header
 */

#endif /* S3HEADER_H */
