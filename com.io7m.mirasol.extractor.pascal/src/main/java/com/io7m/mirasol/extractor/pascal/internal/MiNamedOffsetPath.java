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


package com.io7m.mirasol.extractor.pascal.internal;

import com.io7m.mirasol.core.MiSimpleName;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A path to an identifier.
 *
 * @param values The identifier path
 */

record MiNamedOffsetPath(
  List<MiNamedOffset> values)
{
  /**
   * A path to an identifier.
   *
   * @param values The identifier path
   */

  MiNamedOffsetPath
  {
    values = List.copyOf(values);
  }

  /**
   * @param offset The new element
   *
   * @return The current path with a new element at the end
   */

  public MiNamedOffsetPath with(
    final MiNamedOffset offset)
  {
    final var newPath = new ArrayList<>(this.values);
    newPath.add(offset);
    return new MiNamedOffsetPath(newPath);
  }

  /**
   * @return This path as an uppercase C name
   */

  public String toCName()
  {
    return this.values.stream()
      .map(x -> x.name().value())
      .collect(Collectors.joining("_"));
  }

  @Override
  public String toString()
  {
    return this.values.stream()
      .map(MiNamedOffset::name)
      .map(MiSimpleName::value)
      .collect(Collectors.joining("."));
  }
}
