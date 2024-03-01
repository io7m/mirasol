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


package com.io7m.mirasol.extractor.cpp.internal;

import com.io7m.mirasol.core.MiPackageType;
import com.io7m.mirasol.extractor.api.MiExtractorConfiguration;
import com.io7m.mirasol.extractor.api.MiExtractorType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Objects;

/**
 * An extractor for C/C++.
 */

public final class MiExtractorCPP
  implements MiExtractorType
{
  private static final OpenOption[] OPEN_OPTIONS = {
    StandardOpenOption.CREATE,
    StandardOpenOption.WRITE,
    StandardOpenOption.TRUNCATE_EXISTING,
  };

  private final MiExtractorConfiguration configuration;

  /**
   * An extractor for C/C++.
   *
   * @param inConfiguration The configuration
   */

  public MiExtractorCPP(
    final MiExtractorConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
  }

  @Override
  public void execute()
    throws IOException
  {
    for (final var pack : this.configuration.packageList()) {
      this.executePackage(pack);
    }
  }

  private void executePackage(
    final MiPackageType pack)
    throws IOException
  {
    final var fileName =
      fileNameOf(pack);
    final var path =
      this.configuration.outputDirectory().resolve(fileName);

    try (var writer = Files.newBufferedWriter(path, OPEN_OPTIONS)) {
      this.executePackageFile(pack, writer);
    }
  }

  private void executePackageFile(
    final MiPackageType pack,
    final BufferedWriter writer)
    throws IOException
  {
    final var guardName = guardNameOf(pack);
    writer.append("#ifndef ");
    writer.append(guardName);
    writer.append('\n');

    writer.append("#define ");
    writer.append(guardName);
    writer.append('\n');
    writer.append('\n');

    writer.append("#include <stdint.h>\n");
    writer.append('\n');

    writer.append("#endif // ");
    writer.append(guardName);
    writer.append('\n');
  }

  private static String guardNameOf(
    final MiPackageType pack)
  {
    return "%s_H".formatted(
      pack.name()
        .toString()
        .replace('.', '_')
        .toUpperCase(Locale.ROOT)
    );
  }

  private static String fileNameOf(
    final MiPackageType pack)
  {
    return "%s.h".formatted(
      pack.name().toString().replace('.', '_')
    );
  }
}
