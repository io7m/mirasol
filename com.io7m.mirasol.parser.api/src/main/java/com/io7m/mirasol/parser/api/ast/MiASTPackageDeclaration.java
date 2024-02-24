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


package com.io7m.mirasol.parser.api.ast;

import com.io7m.jlexing.core.LexicalPosition;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * A package declaration.
 *
 * @param lexical       The lexical info
 * @param documentation The documentation annotation
 * @param name          The name
 * @param elements      The elements
 */

public record MiASTPackageDeclaration(
  LexicalPosition<URI> lexical,
  MiASTDocumentation documentation,
  MiASTPackageName name,
  List<MiASTPackageElementType> elements)
  implements MiASTDocumentedType
{
  /**
   * A package declaration.
   *
   * @param lexical       The lexical info
   * @param documentation The documentation annotation
   * @param name          The name
   * @param elements      The elements
   */

  public MiASTPackageDeclaration
  {
    Objects.requireNonNull(lexical, "lexical");
    Objects.requireNonNull(documentation, "documentation");
    Objects.requireNonNull(name, "name");
    elements = List.copyOf(elements);
  }
}
