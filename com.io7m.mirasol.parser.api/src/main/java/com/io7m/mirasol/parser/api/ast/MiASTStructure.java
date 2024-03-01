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
import java.util.Optional;

/**
 * A structure declaration.
 *
 * @param lexical       The lexical position
 * @param documentation The documentation annotation
 * @param sizeAssertion The size assertion
 * @param name          The structure name
 * @param fields        The structure fields
 */

public record MiASTStructure(
  LexicalPosition<URI> lexical,
  MiASTDocumentation documentation,
  Optional<MiASTSizeAssertion> sizeAssertion,
  MiASTSimpleName name,
  List<MiASTFieldType> fields)
  implements MiASTDocumentedType, MiASTPackageElementType, MiASTTypeElementType
{
  /**
   * A structure declaration.
   *
   * @param lexical       The lexical position
   * @param documentation The documentation annotation
   * @param sizeAssertion The size assertion
   * @param name          The structure name
   * @param fields        The structure fields
   */

  public MiASTStructure
  {
    Objects.requireNonNull(lexical, "lexical");
    Objects.requireNonNull(documentation, "documentation");
    Objects.requireNonNull(sizeAssertion, "sizeAssertion");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(fields, "fields");
  }
}
