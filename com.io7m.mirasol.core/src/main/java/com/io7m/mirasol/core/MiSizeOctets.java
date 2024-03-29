/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.mirasol.core;

import java.math.BigInteger;

/**
 * A size in octets.
 *
 * @param value The value
 */

public record MiSizeOctets(
  BigInteger value)
  implements Comparable<MiSizeOctets>
{
  /**
   * A size in octets.
   *
   * @param value The value
   */

  public MiSizeOctets
  {
    if (value.compareTo(BigInteger.ZERO) < 0) {
      throw new IllegalArgumentException("Sizes must be non-negative.");
    }
  }

  @Override
  public String toString()
  {
    return this.value.toString();
  }

  @Override
  public int compareTo(
    final MiSizeOctets other)
  {
    return this.value.compareTo(other.value);
  }

  /**
   * Convenience method to construct sizes.
   *
   * @param value The size value
   *
   * @return The size value
   */

  public static MiSizeOctets of(
    final long value)
  {
    return new MiSizeOctets(BigInteger.valueOf(value));
  }
}
