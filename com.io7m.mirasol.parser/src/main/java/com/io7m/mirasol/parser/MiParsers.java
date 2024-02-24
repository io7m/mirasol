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


package com.io7m.mirasol.parser;

import com.io7m.anethum.api.ParseStatus;
import com.io7m.mirasol.parser.api.MiLexical;
import com.io7m.mirasol.parser.api.MiParserFactoryType;
import com.io7m.mirasol.parser.api.MiParserType;
import com.io7m.mirasol.parser.internal.MiParser;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The default parser factory.
 */

public final class MiParsers implements MiParserFactoryType
{
  /**
   * The default parser factory.
   */

  public MiParsers()
  {

  }

  @Override
  public MiParserType createParserWithContext(
    final MiLexical context,
    final URI source,
    final InputStream stream,
    final Consumer<ParseStatus> statusConsumer)
  {
    return new MiParser(
      source,
      stream,
      statusConsumer,
      Objects.requireNonNullElse(context, MiLexical.PRESERVE_LEXICAL)
    );
  }
}
