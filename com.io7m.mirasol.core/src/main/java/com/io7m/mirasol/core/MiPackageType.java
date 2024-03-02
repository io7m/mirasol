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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * A package declaration.
 */

public interface MiPackageType
  extends Collection<MiPackageElementType>, MiDocumentedType
{
  /**
   * @return The package name
   */

  MiPackageName name();

  /**
   * @return The list of imported packages
   */

  List<MiPackageReference> imports();

  /**
   * @param name The type name
   *
   * @return The type, if one exists
   */

  Optional<MiTypeReference> type(
    MiSimpleName name);

  /**
   * @param name The object name
   *
   * @return The object, if one exists
   */

  Optional<MiPackageElementType> object(
    MiSimpleName name);

  /**
   * @return The maps in alphabetical order
   */

  Collection<MiMapType> maps();

  /**
   * @return The types in alphabetical order
   */

  Collection<MiTypeType> types();

  /**
   * @return The types in topological order
   */

  Collection<MiTypeType> typesTopological();
}
