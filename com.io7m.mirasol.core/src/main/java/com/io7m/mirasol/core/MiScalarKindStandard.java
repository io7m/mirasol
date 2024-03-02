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

import java.util.Objects;

/**
 * The standard scalar kinds.
 */

public enum MiScalarKindStandard
  implements MiScalarKindType
{
  /**
   * Standard signed integers.
   */

  INTEGER_SIGNED("IntegerSigned"),

  /**
   * Standard unsigned integers.
   */

  INTEGER_UNSIGNED("IntegerUnsigned");

  private final String humanName;

  MiScalarKindStandard(
    final String inName)
  {
    this.humanName =
      Objects.requireNonNull(inName, "inName");
  }

  @Override
  public String show()
  {
    return this.humanName;
  }
}
