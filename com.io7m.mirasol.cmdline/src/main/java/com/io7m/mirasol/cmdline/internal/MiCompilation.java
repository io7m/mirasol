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


package com.io7m.mirasol.cmdline.internal;

import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.mirasol.compiler.MiCompilers;
import com.io7m.mirasol.compiler.api.MiCompilerResultType;
import com.io7m.mirasol.core.MiPackageType;
import com.io7m.mirasol.loader.api.MiLoaderType;
import com.io7m.seltzer.api.SStructuredErrorType;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class MiCompilation
{
  private MiCompilation()
  {

  }

  static void logError(
    final Logger logger,
    final SStructuredErrorType<String> error)
  {
    var maxKeyLength = 0;
    for (final var entry : error.attributes().entrySet()) {
      maxKeyLength = Math.max(maxKeyLength, entry.getKey().length());
    }

    final var builder = new StringBuilder(256);
    builder.append(error.errorCode());
    builder.append(": ");
    builder.append(error.message());
    builder.append(System.lineSeparator());

    for (final var entry : error.attributes().entrySet()) {
      final var key = entry.getKey();
      final var pad = maxKeyLength - key.length();
      builder.append("  ");
      builder.append(key);
      builder.append(" ".repeat(pad));
      builder.append(" : ");
      builder.append(entry.getValue());
      builder.append(System.lineSeparator());
    }

    final var actionOpt = error.remediatingAction();
    if (actionOpt.isPresent()) {
      final var action = actionOpt.get();
      builder.append(System.lineSeparator());
      builder.append("Action: ");
      builder.append(action);
      builder.append(System.lineSeparator());
    }

    logger.error("{}", builder);

    if (error.exception().isPresent()) {
      logger.error("", error.exception().get());
    }
  }

  static ArrayList<MiPackageType> doCompile(
    final Logger logger,
    final MiCompilers compilers,
    final MiLoaderType loader,
    final List<Path> files)
    throws MiCompilationFailed
  {
    final var compiler =
      compilers.create(loader);

    final var packages =
      new ArrayList<MiPackageType>();

    final var exceptionTracker =
      new ExceptionTracker<MiCompilationFailed>();

    for (final var file : files) {
      try {
        final MiCompilerResultType<MiPackageType> result =
          compiler.compileFile(file);

        switch (result) {
          case final MiCompilerResultType.Succeeded<MiPackageType> succeeded -> {
            packages.add(succeeded.result());
          }
          case final MiCompilerResultType.Failed<MiPackageType> failed -> {
            exceptionTracker.addException(new MiCompilationFailed());
            for (final var error : failed.errors()) {
              logError(logger, error);
            }
          }
        }
      } catch (final IOException e) {
        logger.error("I/O error: {}: ", file, e);
        exceptionTracker.addException(new MiCompilationFailed());
      }
    }

    exceptionTracker.throwIfNecessary();
    return packages;
  }

  static class MiCompilationFailed extends Exception
  {
    MiCompilationFailed()
    {

    }
  }
}
