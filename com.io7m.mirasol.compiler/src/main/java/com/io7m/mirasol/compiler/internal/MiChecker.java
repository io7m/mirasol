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


package com.io7m.mirasol.compiler.internal;

import com.io7m.mirasol.compiler.api.MiCompilerResultType;
import com.io7m.mirasol.core.MiPackageType;
import com.io7m.mirasol.loader.api.MiLoaderType;
import com.io7m.mirasol.parser.api.ast.MiASTPackageDeclaration;
import com.io7m.mirasol.strings.MiStrings;

import java.util.List;
import java.util.Objects;

/**
 * A binding and type checker.
 */

public final class MiChecker
{
  private final MiASTPackageDeclaration source;
  private final MiStrings strings;
  private final MiLoaderType loader;

  /**
   * A binding and type checker.
   *
   * @param inStrings The strings
   * @param inLoader  The loader
   * @param inSource  The source
   */

  public MiChecker(
    final MiStrings inStrings,
    final MiLoaderType inLoader,
    final MiASTPackageDeclaration inSource)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "inStrings");
    this.loader =
      Objects.requireNonNull(inLoader, "inLoader");
    this.source =
      Objects.requireNonNull(inSource, "source");
  }

  /**
   * @return The result of checking the package
   */

  public MiCompilerResultType<MiPackageType> check()
  {
    final var context =
      new MiCheckerContext(
        this.strings,
        this.loader,
        this.source
      );

    for (final var pass : List.of(
      new MiCheckerPassImports(),
      new MiCheckerPassBindings(),
      new MiCheckerPassSizes(),
      new MiCheckerPassBuild()
    )) {
      try {
        pass.execute(context);
      } catch (final MiCheckerException e) {
        return new MiCompilerResultType.Failed<>(context.errors());
      }
    }

    return new MiCompilerResultType.Succeeded<>(context.createPackage());
  }
}
