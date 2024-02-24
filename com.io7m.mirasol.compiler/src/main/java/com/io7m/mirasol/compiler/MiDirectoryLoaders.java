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

import com.io7m.mirasol.compiler.internal.MiDirectoryLoader;
import com.io7m.mirasol.loader.api.MiLoaderFactoryType;
import com.io7m.mirasol.loader.api.MiLoaderType;
import com.io7m.mirasol.strings.MiStrings;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * A loader implementation that reads package files from a directory.
 */

public final class MiDirectoryLoaders implements MiLoaderFactoryType
{
  private final MiCompilers compilers;
  private final Path directory;
  private final MiStrings strings;

  /**
   * A loader implementation that reads package files from a directory.
   *
   * @param inDirectory The directory
   */

  public MiDirectoryLoaders(
    final Path inDirectory)
  {
    this.directory =
      Objects.requireNonNull(inDirectory, "directory");
    this.compilers =
      new MiCompilers();
    this.strings =
      MiStrings.create(Locale.getDefault());
  }

  @Override
  public MiLoaderType create()
  {
    return new MiDirectoryLoader(
      this,
      this.strings,
      this.compilers,
      this.directory
    );
  }
}
