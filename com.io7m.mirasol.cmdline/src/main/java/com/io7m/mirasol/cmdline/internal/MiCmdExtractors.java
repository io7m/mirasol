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

import com.io7m.mirasol.extractor.api.MiExtractorFactoryType;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType.QConstant;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * {@code extractors}
 */

public final class MiCmdExtractors implements QCommandType
{
  /**
   * {@code extractors}
   */

  public MiCmdExtractors()
  {

  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(

    );
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
  {
    final var extractors =
      ServiceLoader.load(MiExtractorFactoryType.class)
        .stream()
        .map(ServiceLoader.Provider::get)
        .sorted(Comparator.comparing(MiExtractorFactoryType::name))
        .toList();

    final var output = context.output();
    output.println("# Extractor | Language | Description");

    for (final var extractor : extractors) {
      output.print(extractor.name());
      output.print(" | ");
      output.print(extractor.language());
      output.print(" | ");
      output.print(extractor.description());
      output.println();
    }

    return QCommandStatus.SUCCESS;
  }

  @Override
  public QCommandMetadata metadata()
  {
    return new QCommandMetadata(
      "extractors",
      new QConstant("List the available language extractors."),
      Optional.empty()
    );
  }
}
