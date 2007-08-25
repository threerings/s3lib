/*
 * S3Lib.c vi:ts=4:sw=4:expandtab:
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

#include <libxml/parser.h>

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <S3Lib.h>

/**
 * @file
 * @brief S3 library support.
 * @author Landon Fuller <landonf@threerings.net>
 */

/*!
 * @ingroup S3Library
 * @{
 */

/** @internal Set to true if debugging is enabled */
static bool debug = false;

/**
 * Perform global library initialization. This
 * does not initialize libcurl.
 */
TR_DECLARE void s3lib_global_init (void) {
    /* Verify libxml version compatibility. */
    LIBXML_TEST_VERSION;
}

/**
 * Enable/disable verbose debugging output to stderr.
 * @param flag Set to true to enable writing of library debugging output to stderr.
 */
TR_DECLARE void s3lib_enable_debugging (bool flag) {
    debug = flag;
}

/**
 * @internal
 * Returns true if stderr debugging mode output is enabled.
 */
TR_PRIVATE bool s3lib_debugging () {
    return debug;
}

/*!
 * @} S3Library
 */