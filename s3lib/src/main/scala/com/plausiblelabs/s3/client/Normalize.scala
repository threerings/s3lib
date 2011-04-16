/**
 * Normalization implementation from the Extensible Text Framework:
 *   http://sourceforge.net/projects/xtf/
 *
 * Copyright (c) 2009, Plausible Labs Cooperative, Inc.
 * Copyright (c) 2004, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the University of California nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
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

package com.plausiblelabs.s3.client

/**
 * Path normalization.
 */
private[client] object Normalize {

  /**
   * Safely append a parent and child path.
   *
   * - The parent path and child path are normalized independently to ensure
   * that any relative paths are fully resolved.
   * - The two paths are then appended, and a final normalization
   * resolves any duplicate path delimiters that are found.
   */
  def subpath (parent:String, child:String): String = {
    path(path(parent) + "/" + path(child))
  }

  /**
   *  Normalize the specified file system path.
   *
   *  This function performs a number of "cleanup" operations to create
   *  a standardized (or normalized) path. These operations include:
   *
   *  - Stripping any leading or trailing spaces from the passed in path.
   *
   *  - Removes any leading relative traversals, eg, "../../xxx" -> /xxx
   *
   *  - Removes any double slash characters that may have been created
   *    when two partial path strings were concatenated.
   *
   *  - Removes any occurrences of "./"
   *
   *  - Removes any occurrences of "xxx/../"
   *
   *  @param path The path to normalize.
   *
   *  @return A normalized version of the original path string passed.
   */
  def path (path:String): String = {
    /* Create a buffer in which we can normalize the Path. */
    val trimPath = new StringBuilder();

    /* Remove any leading or trailing whitespace from the Path. */
    trimPath.append(path.trim());

    /* Determine the length of the Path. */
    var len = trimPath.length;
    var lastSlash = -1;
    var lastLastSlash = -1;
    var i = 0;

    /* If empty, simply return a blank string */
    if (len == 0)
      return ""

    while (i < len && i >= 0) {
      assert(len == trimPath.length());

      /* Remove any double slashes created by concatenating partial
       * normalized paths together. */
      if (i < len - 1 && trimPath.charAt(i) == '/' && trimPath.charAt(i + 1) == '/')
      {
        trimPath.deleteCharAt(i);
        len = len - 1;
        i = i - 1;
      } else {
        /* Remove ./ */
        if (i < len - 1 &&
            i == lastSlash + 1 &&
            trimPath.charAt(i) == '.' &&
            trimPath.charAt(i + 1) == '/')
        {
          trimPath.delete(i, i + 2);
          len -= 2;
        }

        /* Remove xxx/../ and ../../[xxx] */
        if (i < len - 2 &&
            i == lastSlash + 1 &&
            trimPath.charAt(i) == '.' &&
            trimPath.charAt(i + 1) == '.' &&
            trimPath.charAt(i + 2) == '/')
        {
          if (lastLastSlash >= 0) {
            trimPath.delete(lastLastSlash, i + 2);
            len -= (i + 2 - lastLastSlash);
            i = lastLastSlash;
            lastSlash = trimPath.lastIndexOf("/", lastLastSlash - 1);
          } else {
            /* First ../ at start of path. Attempted traversal beyond
             * the path -- we strip off the '..' and leave '/' */
            trimPath.delete(i, i + 2);
            len -= (i + 2);
            lastSlash = 0;
            lastLastSlash = 0;
          }
        }

        if (trimPath.charAt(i) == '/') {
          lastLastSlash = lastSlash;
          lastSlash = i;
        }
      
        i = i + 1;
      }
    }
    assert(len == trimPath.length())

    /* Return the resulting normalized Path. */
    return trimPath.toString();
  }
}
