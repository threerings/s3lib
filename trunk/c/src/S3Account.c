/*
 * S3Account.c vi:ts=4:sw=4:expandtab:
 * Amazon S3 Library
 *
 * Author: Landon Fuller <landonf@threerings.net>
 *
 * Copyright (c) 2005 - 2008 Landon Fuller <landonf@bikemonkey.org>
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

#include "S3Lib.h"

/**
 * @file
 * @brief S3 User Account Credentials
 * @author Landon Fuller <landonf@threerings.net>
 */

/**
 * @defgroup S3Account User Account Credentials
 * @ingroup S3Library
 * @{
 */

static void s3account_dealloc (S3TypeRef object);

/**
 * S3 User Account Credentials.
 * The S3Account object manages the AWS access identifier and secret key required
 * to issue S3 requests.
 *
 * @sa s3account_new()
 */
struct S3Account {
    S3RuntimeBase base;

    /** @internal
     * AWS ID */
    S3String *awsId;

    /** @internal
     * AWS Access Key. */
    S3String *awsKey;
};

/**
 * @internal
 * S3Account Class Definition
 */
static S3RuntimeClass S3AccountClass = {
    .dealloc = s3account_dealloc
};

/**
 * Instantiate a new S3Account instance.
 *
 * @param awsId AWS access identifier.
 * @param awsKey AWS access key.
 * @return A new S3Account instance, or NULL on failure.
 */
S3_DECLARE S3Account *s3account_new (S3String *awsId, S3String *awsKey) {
    S3Account *account;

    /* Allocate a new S3 Account. */
    account = s3_object_alloc(&S3AccountClass, sizeof(S3Account));
    if (account == NULL)
        return NULL;

    /* id */
    account->awsId = s3_retain(awsId);

    /* key */
    account->awsKey = s3_retain(awsKey);

    return (account);
}

/**
 * @internal
 *
 * S3Account deallocation callback.
 * @warning Do not call directly, use #s3_release
 * @param req A S3Account instance.
 */
static void s3account_dealloc (S3TypeRef object) {
    S3Account *account = (S3Account *) object;

    if (account->awsId != NULL)
        s3_release(account->awsId);

    if (account->awsKey != NULL)
        s3_release(account->awsKey);
}

/*!
 * @} S3Account
 */
