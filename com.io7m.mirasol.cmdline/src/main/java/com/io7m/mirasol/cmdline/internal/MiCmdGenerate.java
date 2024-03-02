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
import com.io7m.mirasol.core.MiPackageType;
import com.io7m.mirasol.extractor.api.MiExtractorConfiguration;
import com.io7m.mirasol.extractor.api.MiExtractorException;
import com.io7m.mirasol.extractor.api.MiExtractorFactoryType;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamed0N;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType.QConstant;
import com.io7m.quarrel.ext.logback.QLogback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

import static com.io7m.mirasol.cmdline.internal.MiCompilation.logError;

/**
 * {@code generate}
 */

public final class MiCmdGenerate implements QCommandType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(MiCmdGenerate.class);

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

  private static final QParameterNamed1<Path> OUTPUT_DIRECTORY =
    new QParameterNamed1<>(
      "--output-directory",
      List.of(),
      new QConstant("The output directory."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<String> EXTRACTOR =
    new QParameterNamed1<>(
      "--extractor",
      List.of(),
      new QConstant("The extractor."),
      Optional.empty(),
      String.class
    );

  /**
   * {@code generate}
   */

  public MiCmdGenerate()
  {

  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return QLogback.plusParameters(
      List.of(
        EXTRACTOR,
        FILES,
        OUTPUT_DIRECTORY,
        PACKAGE_DIRECTORIES
      )
    );
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws IOException
  {
    QLogback.configure(context);

    final var files =
      context.parameterValues(FILES);
    final var packageDirectories =
      context.parameterValues(PACKAGE_DIRECTORIES);
    final var outputDirectory =
      context.parameterValue(OUTPUT_DIRECTORY);
    final var extractorName =
      context.parameterValue(EXTRACTOR);

    final var extractorsOpt =
      ServiceLoader.load(MiExtractorFactoryType.class)
        .stream()
        .map(ServiceLoader.Provider::get)
        .filter(p -> Objects.equals(p.name(), extractorName))
        .findFirst();

    if (extractorsOpt.isEmpty()) {
      LOG.error("No such extractor exists.");
      return QCommandStatus.FAILURE;
    }

    final var extractors =
      extractorsOpt.get();

    final var directories =
      MiDirectories.create();
    final var packageDirectoriesAll =
      new ArrayList<>(packageDirectories);
    final var systemPackageDirectory =
      directories.dataDirectory().resolve("packages");

    Files.createDirectories(systemPackageDirectory);
    packageDirectoriesAll.addFirst(systemPackageDirectory);

    final var loader =
      new MiDirectoryLoaders(packageDirectoriesAll)
        .create();

    final ArrayList<MiPackageType> packages;
    try {
      packages = MiCompilation.doCompile(LOG, new MiCompilers(), loader, files);
    } catch (final MiCompilation.MiCompilationFailed e) {
      return QCommandStatus.FAILURE;
    }

    packages.addAll(loader.loadedPackages());

    final var extractor =
      extractors.create(new MiExtractorConfiguration(packages, outputDirectory));

    try {
      extractor.execute();
    } catch (final MiExtractorException e) {
      logError(LOG, e);
      for (final var error : e.extras()) {
        logError(LOG, error);
      }
      return QCommandStatus.FAILURE;
    }

    return QCommandStatus.SUCCESS;
  }

  @Override
  public QCommandMetadata metadata()
  {
    return new QCommandMetadata(
      "generate",
      new QConstant("Compile sources and generate source code."),
      Optional.empty()
    );
  }
}
