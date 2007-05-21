/*
 * S3Error.c vi:ts=4:sw=4:expandtab:
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

#include <stdlib.h>
#include <libxml/parser.h>

#include <S3Lib.h>

/**
 * @file
 * @brief S3 error handling.
 * @author Landon Fuller <landonf@threerings.net>
 */

/*!
 * @defgroup S3Error S3 Error Handling
 * @ingroup S3Library
 * @{
 */

/**
 * Stores an S3 error result.
 */
struct S3Error {
    char *code;
    char *message;
    char *resource;
    char *request_id;
};

/**
 * Instantiate a new S3Error instance.
 *
 * @param xmlBuffer S3 XML error document.
 * @param length buffer length.
 */
TR_DECLARE S3Error *s3error_new (const char *xmlBuffer, int length) {
    S3Error *error = NULL;
    xmlDoc *doc = NULL;

    /* Allocate a new S3 error. */
    error = calloc(1, sizeof(S3Error));
    if (error == NULL)
        return NULL;

    /* Parse the error document. */
    doc = xmlReadMemory(xmlBuffer, length, "", NULL, XML_PARSE_NONET | XML_PARSE_NOERROR);
    if (!doc)
        goto error;

    /* Free the document and return the error instance. */
    xmlFreeDoc(doc);
    return error;

error:
    if (doc)
        xmlFreeDoc(doc);

    s3error_free(error);
    return NULL;
}

/**
 * Deallocate a S3 error instance.
 * @param error An S3Error instance.
 */
TR_DECLARE void s3error_free (S3Error *error) {
    if (error->code != NULL)
        free(error->code);
    
    if (error->message != NULL)
        free(error->message);

    if (error->resource != NULL)
        free(error->resource);
    
    if (error->request_id != NULL)
        free(error->request_id);

    free(error);
}

/*!
 * @} S3Error
 */