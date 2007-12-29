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

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <libxml/parser.h>
#include <assert.h>

#include "S3Lib.h"

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
S3_DECLARE void s3lib_global_init (void) {
    /* Verify libxml version compatibility. */
    LIBXML_TEST_VERSION;
}

/**
 * Enable/disable verbose debugging output to stderr.
 * @param flag Set to true to enable writing of library debugging output to stderr.
 */
S3_DECLARE void s3lib_enable_debugging (bool flag) {
    debug = flag;
}

/**
 * @internal
 * Returns true if stderr debugging mode output is enabled.
 */
S3_PRIVATE bool s3lib_debugging () {
    return debug;
}

/*!
 * @} S3Library
 */
 
/*!
 * @defgroup S3LibraryMemory Memory Management
 * @ingroup S3Library
 * @{
 */

/**
 * @internal
 *
 * Allocate and initialize the class definition of a new S3 object instance.
 *
 * The object is zero-filled. The reference count to 1 (implicit reference);
 *
 * @param object Object to initialize.
 * @param class Object's class definition.
 */
S3_PRIVATE S3TypeRef s3_object_alloc (S3RuntimeClass *class, size_t objectSize) {
    S3TypeRef object;
    S3RuntimeBase *objdata;

    object = calloc(1, objectSize);
    if (object == NULL)
        return NULL;

    /* Initialize the object */
    objdata = (S3RuntimeBase *) object;
    objdata->refCount = 1;
    objdata->class = class;

    return object;
}

/**
 * Increments @a object's reference count by one. Should always be complemented by a call
 * to s3_release.
 *
 * @result Returns a reference to @a object.
 */
S3_DECLARE S3TypeRef s3_retain (S3TypeRef object) {
    assert(s3_reference_count(object) != UINT32_MAX);

    ((S3RuntimeBase *) object)->refCount++;
    return object;
} 


/**
 * Return @a object's reference count.
 * This is generally only useful for debugging purposes.
 *
 * @result The provided @a instance's reference count.
 */
S3_DECLARE uint32_t s3_reference_count (S3TypeRef object) {
    return ((S3RuntimeBase *) object)->refCount;
}


/**
 * Decrement the @a instance's reference count by one. When the reference
 * count reaches zero, the object will be immediately deallocated.
 *
 * @param object Object instance to deallocate.
 */
S3_DECLARE void s3_release (S3TypeRef object) {
    S3RuntimeBase *objdata;

    assert(s3_reference_count(object) > 0);

    objdata = (S3RuntimeBase *) object;
    objdata->refCount--;

    if (objdata->refCount == 0) {
        objdata->class->dealloc(object);
        free(object);
    }
}


/*!
 * @} S3LibraryMemory
 */


/*!
 * @defgroup S3LibraryCompare Identifying and Comparing Instances
 * @ingroup S3Library
 * @{
 */

/**
 * @internal
 * Default hash implementation -- returns the address of the object.
 */
static long s3_default_hash (S3TypeRef object) {
    return (long) object;
}

/**
 * @internal
 * Default equals implementation -- compares the addresses of the objects.
 */
static bool s3_default_equals (S3TypeRef self, S3TypeRef other) {
    return (self == other);
}

/**
 * Returns an integer that may be used as a table address in a hash
 * table structure.
 */
S3_DECLARE long s3_hash (S3TypeRef object) {
    S3RuntimeBase *objdata;
    objdata = (S3RuntimeBase *) object;

    if (objdata->class->hash != NULL)
        return objdata->class->hash(object);
    else
        return s3_default_hash(object);
}

/**
 * Compare two objects, returning true if they are equal in value.
 *
 * @note The default comparison function uses the memory address
 * to determine equality. Subclasses will override this to implement 
 * value equality comparison.
 *
 * @param self Object instance that will perform the comparison
 * against @a other. For example, if a S3String is provided,
 * S3String's comparison function will be used.
 * @param other The object instance that the @a self parameter will be
 * compared against.
 */
S3_DECLARE bool s3_equals (S3TypeRef self, S3TypeRef other) {
    S3RuntimeBase *objdata = (S3RuntimeBase *) self;

    if (objdata->class->equals != NULL)
        return objdata->class->equals(self, other);
    else
        return s3_default_equals(self, other);
}

/**
 * @internal
 *
 * Return true if the object is an instance of the given class.
 * Currently uses simple pointer comparison.
 */
S3_PRIVATE bool s3_instanceof (S3TypeRef object, S3RuntimeClass *class) {
    S3RuntimeBase *objdata = (S3RuntimeBase *) object;

    /* Simple pointer comparison */
    return (objdata->class == class);
}


/*!
 * @} S3LibraryCompare
 */


