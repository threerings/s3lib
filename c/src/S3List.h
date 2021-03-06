/*
 * S3List.h vi:ts=4:sw=4:expandtab:
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

#ifndef S3LIST_H
#define S3LIST_H

/**
 * @file
 * @brief S3Lib list implementation.
 * @author Landon Fuller <landonf@threerings.net>
 */

/*!
 * @addtogroup S3List
 * @{
 */

typedef struct S3List S3List;
typedef struct S3ListIterator S3ListIterator;


/**
 * The comparison function is used to compare two list elements, and should return
 * an integer greater than, equal to, or less than 0, if the element elem1
 * is greater than, equal to, or less than the element elem2.
 */
typedef int (*s3list_compare_t) (const S3TypeRef elem1, const S3TypeRef elem2, const void *context);

S3_EXTERN S3List *s3list_new ();
S3_EXTERN S3List *s3list_copy (S3List *list);
S3_EXTERN bool s3list_append (S3List *list, S3TypeRef object);
S3_EXTERN void s3list_sort (S3List *list, s3list_compare_t func, const void *context);

S3_EXTERN S3ListIterator *s3list_iterator_new (S3List *list);
S3_EXTERN S3TypeRef s3list_iterator_next (S3ListIterator *iterator);
S3_EXTERN bool s3list_iterator_hasnext (S3ListIterator *iterator);

#ifdef S3LIB_PRIVATE_API
S3_PRIVATE int s3list_lexicographical_compare (S3TypeRef elem1, S3TypeRef elem2, S3_UNUSED const void *context);
#endif /* S3LIB_PRIVATE_API */

/*!
 * @} S3List
 */

#endif /* S3REQUEST_H */
