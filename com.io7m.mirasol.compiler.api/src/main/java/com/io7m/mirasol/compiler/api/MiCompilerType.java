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

package com.io7m.mirasol.compiler.api;

import com.io7m.mirasol.core.MiPackageType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The type of package compilers.
 */

public interface MiCompilerType
{
  /**
   * Compile a package from the given stream.
   *
   * @param source The source URI
   * @param stream The stream
   *
   * @return The package
   */

  MiCompilerResultType<MiPackageType> compile(
    URI source,
    InputStream stream);

  /**
   * Compile a package from the given file.
   *
   * @param file The file
   *
   * @return The package
   *
   * @throws IOException If the file cannot be opened
   */

  default MiCompilerResultType<MiPackageType> compileFile(
    final Path file)
    throws IOException
  {
    try (var stream = Files.newInputStream(file)) {
      return this.compile(file.toUri(), stream);
    }
  }
}
