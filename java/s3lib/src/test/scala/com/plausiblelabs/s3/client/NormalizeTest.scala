/**
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

import org.junit._
import org.junit.Assert._

class NormalizeTest {
  /** Test ignorable /./ path elements */
  @Test
  def singleDot {
    assertEquals("xyz/foo.txt", Normalize.path("xyz/./foo.txt"));
  }

  /** Test sub-path handling */
  @Test
  def testSubpath {
    assertEquals("/safe/path", Normalize.subpath("/safe/unsafe/../", "/unsafe/../path"))
  }

  /** Test handling of double initial slashes */
  @Test
  def testDoubleSlash {
    assertEquals("/file", Normalize.path("//file"));
  }

  /** Test handling of relative paths */
  @Test
  def relativePath {
    assertEquals("foo/bar.txt", Normalize.path("./foo/bar.txt"));

    assertEquals("foo/bar.txt", Normalize.path("foo/bar.txt"));

    assertEquals("/foo/bar.txt", Normalize.path("../foo/bar.txt"));

    assertEquals("/foo/bar.txt", Normalize.path("../../foo/bar.txt"));

    assertEquals("/foo/bar.txt", Normalize.path("../.././foo/bar.txt"));

    assertEquals("/foo/bar/test.txt", Normalize.path("../../foo/bar/test.txt"));
  }

  /** Verify that '..' traversal is handled correctly */
  @Test
  def dotTraversal {
    assertEquals("/usr/foo/bar.txt", Normalize.path("/usr/tmp/../foo/bar.txt"));
    assertEquals("/usr/foo/bar/", Normalize.path("/usr/local/tmp/../../foo/bar/"));
    assertEquals("/usr/.../tmp/", Normalize.path("/usr/.../tmp/"));
    assertEquals("/usr/tmp/", Normalize.path("/usr/./tmp/"));
  }

  /** Verify that trailing slashes are removed as necessary */
  @Test
  def trailingSlash {
    assertEquals("/test/", Normalize.path("/test//"));
    assertEquals("/test/", Normalize.path("/test/"));
    assertEquals("/test/", Normalize.path("/test///"));
  }
}
