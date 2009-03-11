/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.threerings.s3.client;

import org.junit.*;
import static org.junit.Assert.*;

public class MediaTypeTest {
  @Test
  public void testEquals () {
    assertEquals(new MediaType("media", "encoding"), new MediaType("media", "encoding"));
    assertNotSame(new MediaType("m"), new MediaType("m", "t"));
  }

  @Test
  public void testHash () {
    assertEquals(new MediaType("m", "t").hashCode(), new MediaType("m", "t").hashCode());
    assertNotSame(new MediaType("").hashCode(), new MediaType("m").hashCode());
  }
}
