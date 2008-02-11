/*
 * S3AutoreleasePool.c vi:ts=4:sw=4:expandtab:
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

#include <assert.h>
#include <pthread.h>

#include "S3Lib.h"

/**
 * @file
 * @brief S3 Allocation Pool
 * @author Landon Fuller <landonf@threerings.net>
 */

/*!
 * @defgroup S3AutoreleasePool Auto-release Object Pools
 * @ingroup S3Library
 * @{
 */

static void s3autorelease_pool_dealloc (S3TypeRef obj);

/*
 * Design & Data Structures
 *
 * Three different data structures are used to maintain
 * the thread-local autorelease pool stack:
 *
 * ARPoolStack:
 * A simple (linked-list based) stack of S3AutoreleasePool structures. The
 * S3AutoreleasePool manages all to-be-released objects.
 *
 * ARBucket:
 * A linked-list of buckets. Each bucket holds BUCKET_SIZE of to-be-released
 * objects. New buckets are automatically added when the bucket fills.
 *
 * ARThreadLocalData:
 * Contains a single pointer to an AutoreleasePoolStack instance. Referenced
 * by a pthread_key_t. This wrapper structure is used to avoid unnecessary
 * calls to pthread_setspecific() -- a pointer can simply be updated.
 */

/** @internal Number of objects to store in each pool bucket. Selected arbitrarily. */
#define BUCKET_SIZE 128

static pthread_key_t stack_tld_key;

typedef struct ARPoolStack {
    S3AutoreleasePool *pool;
    struct ARPoolStack *next;
} ARPoolStack;

typedef struct ARBucket {
    int count;
    S3TypeRef objects[BUCKET_SIZE];
    struct ARBucket *next;
} ARBucket;

typedef struct ARThreadLocalData {
    ARPoolStack *stack;
} ARThreadLocalData;



/**
 * S3Lib Allocation Pool.
 *
 * S3AutoreleasePool provides an API for implementing per-thread pools of
 * autoreleased objects. When an object is sent the autorelease method,
 * it is added to the current thread's autorelease pool. Per-thread
 * pools are implemented using a stack -- when a new pool is allocated,
 * it is pushed onto the stack, and all new autoreleased objects are
 * placed in that pool.
 *
 * When the autorelease pool is deallocated, all autoreleased objects
 * are sent release messages. You may add an object to an autorelease
 * pool multiple times, and it will be sent multiple release messages.
 *
 * You must deallocate autorelease pools in the same order they were
 * allocated.
 *
 */
struct S3AutoreleasePool {
    S3RuntimeBase base;

    /** @internal Linked-list of buckets containing objects to be released. */
    ARBucket *bucket;
};

/**
 * @internal
 * S3AutoreleasePool Class Definition.
 */
static S3RuntimeClass S3AutoreleasePoolClass = {
    .version = S3_CURRENT_RUNTIME_CLASS_VERSION,
    .dealloc = s3autorelease_pool_dealloc
};

/**
 * @internal
 * Perform global initialization of the S3AutoreleasePool library
 * module.
 */
S3_PRIVATE void s3autorelease_pool_global_init () {
    pthread_key_create(&stack_tld_key, NULL);
}

/**
 * @internal
 * Perform global cleanup of the S3AutoreleasePool library
 * module.
 */
S3_PRIVATE void s3autorelease_pool_global_cleanup () {
    pthread_key_delete(stack_tld_key);
}

/**
 * @internal
 * Allocate and initialize a new bucket, attach it to the
 * supplied bucket.
 */
static ARBucket *bucket_add (ARBucket *bucket) {
    ARBucket *new;

    /* Allocate the bucket */
    new = malloc(sizeof(ARBucket));
    assert(new != NULL); // there's no reasonable way to handle malloc failure here.

    /* Initialize it */
    new->count = 0;
    new->next = bucket;

    return new;
}

/**
 * @internal
 * Free the bucket stack, sending release messages to the contained objects.
 */
static void bucket_flush (ARBucket *bucket) {
    ARBucket *next, *cur;
    int i;
    
    /* Free all buckets */
    cur = bucket; 
    while (cur != NULL) {
        /* Send release message to all objects in the bucket */
        for (i = 0; i < cur->count; i++) {
            s3_release(cur->objects[i]);
        }
        next = cur->next;
        free(cur);
        cur = next;
    }
}


/**
 * Instantiate a new S3AutoreleasePool instance.
 *
 * When the autorelease pool is deallocated, all autoreleased objects
 * are sent release messages. You may add an object to an autorelease
 * pool multiple times, and it will be sent multiple release messages.
 *
 * You must deallocate autorelease pools in the same order they were
 * allocated.
 *
 * @return A new #S3AutoreleasePool instance.
 */
S3_DECLARE S3AutoreleasePool *s3autorelease_pool_new () {
    S3AutoreleasePool *pool;
    ARThreadLocalData *tld;
    ARPoolStack *stack;

    /* Allocate a new S3AutoreleasePool */
    pool = s3_object_alloc(&S3AutoreleasePoolClass, sizeof(S3AutoreleasePool));
    if (pool == NULL)
        return NULL;

    /* Allocate and initialize our bucket */
    pool->bucket = malloc(sizeof(ARBucket));
    if (pool->bucket == NULL)
        goto error;

    pool->bucket->count = 0;
    pool->bucket->next = NULL;

    /*
     * Insert the newly allocated pool onto the TLD stack.
     */

    /* Get the TLD -- Create it if it does not exist */
    if ((tld = pthread_getspecific(stack_tld_key)) == NULL) {
        if ((tld = malloc(sizeof(ARThreadLocalData))) == NULL)
            goto error;

        tld->stack = NULL;
        pthread_setspecific(stack_tld_key, tld);
    }

    /* Allocate our new stack entry */
    stack = malloc(sizeof(ARPoolStack));
    if (stack == NULL)
        goto error;
    stack->pool = pool;

    /* If a current stack exists, append the new one,
     * otherwise, we're the first stack */
    if (tld->stack != NULL)
        /* We are a sub-pool */
        stack->next = tld->stack;
    else
        /* We are the first pool */
        stack->next = NULL;

    /* Set new per-thread pool stack */
    tld->stack = stack;

    return pool;

error:
    s3_release(pool);
    return NULL;
}


/**
 * Add an object to the pool.
 *
 * Any objects added to the pool will be automatically passed to #s3_release when the pool is deallocated. An object
 * may be added several times to the same pool, and when the pool is deallocated, #s3_release will be called
 * for each time it was added.
 *
 * Callers MUST own a reference to @a object.
 *
 * Normally you would not call this method directly, and instead use #s3_autorelease.
 *
 * @param pool The pool to add the object to.
 * @param object The object to add to @a pool.
 */
S3_DECLARE void s3autorelease_pool_add (S3AutoreleasePool *pool, S3TypeRef object) {
    /* If the current bucket is full, create a new one */
    if (pool->bucket->count == BUCKET_SIZE) {
        pool->bucket = bucket_add(pool->bucket);
    }

    pool->bucket->objects[pool->bucket->count] = object;
    pool->bucket->count++;
}

/**
 * @internal
 *
 * Add an object to the current thread's autorelease pool.
 *
 * Generally you should not call this directly, and instead use #s3_autorelease
 */
S3_PRIVATE void s3autorelease_pool_add_current (S3TypeRef object) {
    ARThreadLocalData *tld;

    tld = pthread_getspecific(stack_tld_key);
    if (tld == NULL || tld->stack == NULL) {
        // Programmer error, there MUST be an autorelease pool. Provide a nicer
        // error message.
        fprintf(stderr, "S3LIB: Can not find S3AutoreleasePool for thread, aborting\n");
        assert(tld != NULL && tld->stack != NULL);
        abort();
    }

    s3autorelease_pool_add(tld->stack->pool, object);
}


/**
 * @internal
 *
 * S3AutoreleasePool deallocation callback.
 * @warning Do not call directly, use #s3_release
 * @param ob An #S3AutoreleasePool instance.
 */
static void s3autorelease_pool_dealloc (S3TypeRef obj) {
    S3AutoreleasePool *pool;
    ARPoolStack *stack;
    ARThreadLocalData *tld;
   
    /*
     * Clean up the pool
     */ 
    pool = (S3AutoreleasePool *) obj;
    assert(s3_instanceof(pool, &S3AutoreleasePoolClass));
    if (pool->bucket != NULL)
        bucket_flush(pool->bucket);

    /*
     * Pop this pool off the thread-local stack.
     */
    tld = pthread_getspecific(stack_tld_key);
    stack = tld->stack;
    tld->stack = stack->next;
    free(stack);

    /*
     * If this is the last stack entry, destroy the TLD.
     */
    if (tld->stack == NULL) {
        free(tld);
        pthread_setspecific(stack_tld_key, NULL);
    }
}

/*!
 * @} S3AutoreleasePool
 */
