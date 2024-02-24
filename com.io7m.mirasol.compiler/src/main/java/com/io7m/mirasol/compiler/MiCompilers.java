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


package com.io7m.mirasol.compiler;

import com.io7m.mirasol.compiler.api.MiCompilerFactoryType;
import com.io7m.mirasol.compiler.api.MiCompilerType;
import com.io7m.mirasol.compiler.internal.MiCompiler;
import com.io7m.mirasol.loader.api.MiLoaderType;
import com.io7m.mirasol.parser.api.MiParserFactoryType;
import com.io7m.mirasol.strings.MiStrings;

import java.util.Locale;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * The default compiler factory.
 */

public final class MiCompilers implements MiCompilerFactoryType
{
  private final MiParserFactoryType parsers;
  private final MiStrings strings;

  /**
   * The default compiler factory.
   */

  public MiCompilers()
  {
    this(
      service(MiParserFactoryType.class)
    );
  }

  /**
   * The default compiler factory.
   *
   * @param inParsers The parsers
   */

  public MiCompilers(
    final MiParserFactoryType inParsers)
  {
    this.parsers =
      Objects.requireNonNull(inParsers, "parsers");
    this.strings =
      MiStrings.create(Locale.getDefault());
  }

  private static <T> T service(
    final Class<T> clazz)
  {
    return ServiceLoader.load(clazz)
      .findFirst()
      .orElseThrow(() -> {
        return new IllegalStateException(
          "No services available of type %s".formatted(clazz)
        );
      });
  }

  @Override
  public MiCompilerType create(
    final MiLoaderType loader)
  {
    return new MiCompiler(this.strings, loader, this.parsers);
  }
}
