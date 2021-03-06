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

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <stdlib.h>
#include <string.h>

#include <libxml/parser.h>

#include "S3Lib.h"

/**
 * @file
 * @brief S3 error handling.
 * @author Landon Fuller <landonf@threerings.net>
 */

/*!
 * @defgroup S3Error Error Handling
 * @ingroup S3Library
 * @{
 */

/*!
 * @defgroup S3ServerError Server Error Handling
 * @ingroup S3Error
 * @{
 */

static void s3server_error_dealloc (S3TypeRef obj);

/**
 * Represents a S3 server error result.
 * @attention Operations on a S3Error instance are not guaranteed thread-safe, and
 * a S3Error should not be shared between threads without external synchronization.
 */
struct S3ServerError {
    S3RuntimeBase base;

    /** @internal
     * S3 error code. */
    S3String *code;

    /** @internal
     * S3-generated error message. */
    S3String *message;

    /** @internal
     * Resource requested. */
    S3String *resource;

    /** @internal
     * Associated request id. */
    S3String *requestid;
};

/**
 * @internal
 * S3ServerError Class Definition
 */
static S3RuntimeClass S3ServerErrorClass = {
    .version = S3_CURRENT_RUNTIME_CLASS_VERSION,
    .dealloc = s3server_error_dealloc
};

/**
 * Instantiate a new S3Error instance.
 *
 * @param xmlBuffer S3 XML error document.
 * @return A new #S3ServerError instance.
 */
S3_DECLARE S3ServerError *s3server_error_new (S3String *xmlBuffer) {
    S3ServerError *error = NULL;
    xmlDoc *doc = NULL;
    xmlNode *root;
    xmlNode *node;

    /* Allocate a new S3 error. */
    error = s3_object_alloc(&S3ServerErrorClass, sizeof(S3ServerError));
    if (error == NULL)
        return NULL;

    /* Parse the error document. */
    doc = xmlReadMemory(s3string_cstring(xmlBuffer), s3string_length(xmlBuffer), "noname.xml", NULL, XML_PARSE_NONET | XML_PARSE_NOERROR);
    if (doc == NULL)
        goto error;

    root = xmlDocGetRootElement(doc);
    if (root == NULL || root->children == NULL)
        goto error;

    /* Iterate over the <Error> children elements. We cast back and forth
     * between UTF-8 unsigned char and signed char pointers, for lack of
     * a standard UTF-8 string type. */
    for (node = root->children; node != NULL; node = node->next) {
        /* Must be an element node. */
        if (node->type != XML_ELEMENT_NODE)
            continue;

        /* Must have a child text element */
        if (node->children == NULL || node->children->type != XML_TEXT_NODE)
            continue;

        /* The child element must have text content */
        if (node->children->content == NULL)
            continue;

        /* Code */
        if (xmlStrEqual(node->name, (xmlChar *) "Code")) {
            error->code = s3string_new((const char *) node->children->content);
        }

        /* Message */
        else if (xmlStrEqual(node->name, (xmlChar *) "Message")) {
            error->message = s3string_new((const char *) node->children->content);
        }

        /* Resource */
        else if (xmlStrEqual(node->name, (xmlChar *) "Resource")) {
            error->resource = s3string_new((const char *) node->children->content);
        }
        
        /* RequestId */
        else if (xmlStrEqual(node->name, (xmlChar *) "RequestId")) {
            error->requestid = s3string_new((const char *) node->children->content);
        }
    }

    /* Free the document and return the error instance. */
    xmlFreeDoc(doc);
    return error;

error:
    if (doc)
        xmlFreeDoc(doc);

    s3_release(error);
    return NULL;
}


/**
 * Return the request id associated with the #S3ServerError.
 *
 * @param error A #S3ServerError instance
 * @return The request ID, or NULL if the server did not provide one.
 */
S3_DECLARE S3String *s3server_error_requestid (S3ServerError *error) {
    return error->requestid;
}


/**
 * @internal
 *
 * S3ServerError deallocation callback.
 * @warning Do not call directly, use #s3_release
 * @param error An #S3ServerError instance.
 */
static void s3server_error_dealloc (S3TypeRef obj) {
    S3ServerError *error = (S3ServerError *) obj;

    if (error->code != NULL)
        s3_release(error->code);
    
    if (error->message != NULL)
        s3_release(error->message);

    if (error->resource != NULL)
        s3_release(error->resource);
    
    if (error->requestid != NULL)
        s3_release(error->requestid);
}

/*!
 * @} S3ServerError
 */

/*!
 * @} S3Error
 */
