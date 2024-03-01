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

import com.io7m.mirasol.compiler.MiCompilers;
import com.io7m.mirasol.compiler.MiDirectoryLoaders;
import com.io7m.mirasol.compiler.api.MiCompilerResultType;
import com.io7m.mirasol.core.MiPackageType;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamed0N;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType.QConstant;
import com.io7m.quarrel.ext.logback.QLogback;
import com.io7m.seltzer.api.SStructuredErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code compile}
 */

public final class MiCmdCompile implements QCommandType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(MiCmdCompile.class);

  private static final QParameterNamed0N<Path> FILES =
    new QParameterNamed0N<>(
      "--file",
      List.of(),
      new QConstant("The source file(s) to compile."),
      List.of(),
      Path.class
    );

  private static final QParameterNamed0N<Path> PACKAGE_DIRECTORIES =
    new QParameterNamed0N<>(
      "--package-directory",
      List.of(),
      new QConstant("The source package directories."),
      List.of(),
      Path.class
    );

  /**
   * {@code compile}
   */

  public MiCmdCompile()
  {

  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return QLogback.plusParameters(
      List.of(
        FILES,
        PACKAGE_DIRECTORIES
      )
    );
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
  {
    QLogback.configure(context);

    final var files =
      context.parameterValues(FILES);
    final var packageDirectories =
      context.parameterValues(PACKAGE_DIRECTORIES);

    final var directories =
      MiDirectories.create();
    final var packageDirectoriesAll =
      new ArrayList<>(packageDirectories);
    packageDirectoriesAll.addFirst(directories.dataDirectory());

    final var loader =
      new MiDirectoryLoaders(packageDirectories)
        .create();

    final var compilers =
      new MiCompilers();
    final var compiler =
      compilers.create(loader);

    var failedFlag = false;
    for (final var file : files) {
      try {
        final MiCompilerResultType<MiPackageType> result =
          compiler.compileFile(file);

        switch (result) {
          case final MiCompilerResultType.Succeeded<MiPackageType> ignored -> {
            // OK
          }
          case final MiCompilerResultType.Failed<MiPackageType> failed -> {
            failedFlag = true;
            for (final var error : failed.errors()) {
              logError(LOG, error);
            }
          }
        }
      } catch (final IOException e) {
        LOG.error("I/O error: {}: ", file, e);
        failedFlag = true;
      }
    }

    return failedFlag ? QCommandStatus.FAILURE : QCommandStatus.SUCCESS;
  }

  private static void logError(
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

  @Override
  public QCommandMetadata metadata()
  {
    return new QCommandMetadata(
      "compile",
      new QConstant("Compile sources."),
      Optional.empty()
    );
  }
}
