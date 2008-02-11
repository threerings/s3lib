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

#include <openssl/hmac.h>
#include <openssl/evp.h>
#include <openssl/sha.h>

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
    .version = S3_CURRENT_RUNTIME_CLASS_VERSION,
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
 * Sign a S3 request policy, using the AWS key, returning a base64 SHA1 HMAC. This signature can be
 * passed via the Authorization header or request parameter.
 *
 * http://docs.amazonwebservices.com/AmazonS3/2006-03-01/RESTAuthentication.html
 *
 * @param account Account containing the signing credentials.
 * @param policy Policy to be signed.
 * @return An #S3String containing the base64-encoded HMAC-SHA1 signature.
 */
S3_DECLARE S3String *s3account_sign_policy (S3Account *account, S3String *policy) {
    HMAC_CTX        hmac;
    const EVP_MD    *md;
    unsigned char   output[SHA_DIGEST_LENGTH];
    unsigned int    output_len;

    /* Set up the HMAC context */
    HMAC_CTX_init(&hmac);
    md = EVP_sha1();
    HMAC_Init_ex(&hmac, s3string_cstring(account->awsKey), s3string_length(account->awsKey), md, NULL);
    
    /* Add the policy data */
    HMAC_Update(&hmac, (const unsigned char *) s3string_cstring(policy), s3string_length(policy));

    /* Output the HMAC */
    output_len = sizeof(output);
    HMAC_Final(&hmac, output, &output_len);
    HMAC_CTX_cleanup(&hmac);

    /* Return the base64 encoded signature */
    return s3_base64_encode(output, output_len);
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
