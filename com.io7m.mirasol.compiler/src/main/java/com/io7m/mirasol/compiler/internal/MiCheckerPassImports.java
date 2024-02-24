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

import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.mirasol.core.MiException;
import com.io7m.mirasol.core.MiPackageName;
import com.io7m.mirasol.parser.api.ast.MiASTImportDeclaration;
import com.io7m.mirasol.parser.api.ast.MiASTMap;
import com.io7m.mirasol.parser.api.ast.MiASTScalarTypeDeclaration;
import com.io7m.mirasol.parser.api.ast.MiASTStructure;

/**
 * A checker pass that processes imports.
 */

final class MiCheckerPassImports
  implements MiCheckerPassType
{
  MiCheckerPassImports()
  {

  }

  @Override
  public void execute(
    final MiCheckerContext context)
    throws MiCheckerException
  {
    final var tracker = new ExceptionTracker<MiCheckerException>();

    for (final var element : context.source().elements()) {
      switch (element) {
        case final MiASTImportDeclaration importDeclaration -> {
          try {
            checkImport(context, importDeclaration);
          } catch (final MiCheckerException e) {
            tracker.addException(e);
          }
        }
        case final MiASTMap ignored -> {
          // Nothing
        }
        case final MiASTScalarTypeDeclaration ignored -> {
          // Nothing
        }
        case final MiASTStructure ignored -> {
          // Nothing
        }
      }
    }

    tracker.throwIfNecessary();
  }

  private static void checkImport(
    final MiCheckerContext context,
    final MiASTImportDeclaration importDeclaration)
    throws MiCheckerException
  {
    final var name =
      new MiPackageName(importDeclaration.packageName().value());

    try {
      final var pack =
        context.openPackage(name);
      final var prefix =
        importDeclaration.prefix();

      context.addImport(prefix, pack);
    } catch (final MiException e) {
      context.error(e);
      for (final var x : e.extras()) {
        context.error(x);
      }
      throw new MiCheckerException();
    }
  }
}
