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
import com.io7m.mirasol.core.MiSimpleName;

import java.net.URI;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A simple name declaration.
 *
 * @param lexical The lexical position
 * @param value   The value
 */

public record MiASTSimpleName(
  LexicalPosition<URI> lexical,
  String value)
  implements MiASTElementType
{
  private static final Pattern VALID_NAME =
    Pattern.compile("[a-zA-Z][a-zA-Z0-9_-]{0,63}");

  /**
   * A simple name declaration.
   *
   * @param lexical The lexical position
   * @param value   The value
   */

  public MiASTSimpleName
  {
    Objects.requireNonNull(lexical, "lexical");

    if (!VALID_NAME.matcher(value).matches()) {
      throw new IllegalArgumentException(
        "Names must match %s".formatted(VALID_NAME)
      );
    }
  }

  /**
   * @return This name as a core simple name
   */

  public MiSimpleName toSimpleName()
  {
    return new MiSimpleName(this.value());
  }
}
