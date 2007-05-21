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
#include <string.h>

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
    char *requestid;
};

/**
 * Instantiate a new S3Error instance.
 *
 * @param xmlBuffer S3 XML error document.
 * @param length buffer length.
 * @return A new S3Error instance.
 */
TR_DECLARE S3Error *s3error_new (const char *xmlBuffer, int length) {
    S3Error *error = NULL;
    xmlDoc *doc = NULL;
    xmlNode *root;
    xmlNode *node;

    /* Allocate a new S3 error. */
    error = calloc(1, sizeof(S3Error));
    if (error == NULL)
        return NULL;

    /* Parse the error document. */
    doc = xmlReadMemory(xmlBuffer, length, "noname.xml", NULL, XML_PARSE_NONET | XML_PARSE_NOERROR);
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
            error->code = strdup((char *) node->children->content);
        }

        /* Message */
        else if (xmlStrEqual(node->name, (xmlChar *) "Message")) {
            error->message = strdup((char *) node->children->content);
        }

        /* Resource */
        else if (xmlStrEqual(node->name, (xmlChar *) "Resource")) {
            error->resource = strdup((char *) node->children->content);
        }
        
        /* RequestId */
        else if (xmlStrEqual(node->name, (xmlChar *) "RequestId")) {
            error->requestid = strdup((char *) node->children->content);
        }
    }

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
 * Return the S3Error Request Id.
 *
 * @param error A S3Error instance
 * @return The request ID, or NULL if the server did not provide one.
 */
TR_DECLARE const char *s3error_requestid (S3Error *error) {
    return error->requestid;
}


/**
 * Deallocate a S3Error instance.
 * @param error An S3Error instance.
 */
TR_DECLARE void s3error_free (S3Error *error) {
    if (error->code != NULL)
        free(error->code);
    
    if (error->message != NULL)
        free(error->message);

    if (error->resource != NULL)
        free(error->resource);
    
    if (error->requestid != NULL)
        free(error->requestid);

    free(error);
}

/*!
 * @} S3Error
 */