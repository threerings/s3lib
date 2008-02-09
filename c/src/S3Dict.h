/*
 * S3Dict.h vi:ts=4:sw=4:expandtab:
 * Amazon S3 Library
 *
 * Author: Landon Fuller <landonf@bikemonkey.org>
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

#ifndef S3DICT_H
#define S3DICT_H

/**
 * @file
 * @brief S3Lib Dictionary implementation.
 * @author Landon Fuller <landonf@bikemonkey.org>
 */

/*!
 * @addtogroup S3Dict
 * @{
 */

/* Dictionary */
typedef struct S3Dict S3Dict;

/* Dictionary Iterator */
typedef struct S3DictIterator S3DictIterator;

S3_EXTERN S3Dict *s3dict_new ();

S3_EXTERN bool s3dict_put (S3Dict *dict, S3TypeRef key, S3TypeRef value);
S3_EXTERN S3TypeRef s3dict_get (S3Dict *dict, S3TypeRef key);

S3_EXTERN S3DictIterator *s3dict_iterator_new (S3Dict *dictionary);
S3_EXTERN S3TypeRef s3dict_iterator_next (S3DictIterator *iterator);
S3_EXTERN bool s3dict_iterator_hasnext (S3DictIterator *iterator);

/*!
 * @} S3Dict
 */

#endif /* S3DICT_H */
