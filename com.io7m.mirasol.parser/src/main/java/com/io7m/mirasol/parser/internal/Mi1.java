/*
 * Copyright © 2023 Mark Raynsford <code@io7m.com> https://www.io7m.com
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


package com.io7m.mirasol.parser.internal;

import com.io7m.blackthorne.core.BTQualifiedName;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.mirasol.schema.MiSchemas;
import org.xml.sax.Locator;

import java.net.URI;
import java.util.Optional;

/**
 * Functions over v1 elements.
 */

public final class Mi1
{
  private Mi1()
  {

  }

  /**
   * Translate a lexical position.
   *
   * @param locator The locator
   *
   * @return The position
   */

  public static LexicalPosition<URI> position(
    final Locator locator)
  {
    return LexicalPosition.of(
      locator.getLineNumber(),
      locator.getColumnNumber(),
      Optional.ofNullable(locator.getSystemId())
        .map(URI::create)
    );
  }

  /**
   * The element with the given name.
   *
   * @param localName The local name
   *
   * @return The qualified name
   */

  public static BTQualifiedName element(
    final String localName)
  {
    return BTQualifiedName.of(
      MiSchemas.schema1().namespace().toString(),
      localName
    );
  }

}
