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

import com.io7m.mirasol.compiler.api.MiCompilerFactoryType;
import com.io7m.mirasol.compiler.api.MiCompilerResultType;
import com.io7m.mirasol.compiler.api.MiCompilerResultType.Failed;
import com.io7m.mirasol.compiler.api.MiCompilerResultType.Succeeded;
import com.io7m.mirasol.core.MiException;
import com.io7m.mirasol.core.MiPackageName;
import com.io7m.mirasol.core.MiPackageType;
import com.io7m.mirasol.loader.api.MiLoaderType;
import com.io7m.mirasol.strings.MiStrings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import static com.io7m.mirasol.strings.MiStringConstants.ERROR_CIRCULAR_DEPENDENCY;
import static com.io7m.mirasol.strings.MiStringConstants.ERROR_CIRCULAR_PATH;
import static com.io7m.mirasol.strings.MiStringConstants.ERROR_IO;
import static com.io7m.mirasol.strings.MiStringConstants.ERROR_NO_SUCH_PACKAGE;
import static com.io7m.mirasol.strings.MiStringConstants.PACKAGE;
import static com.io7m.mirasol.strings.MiStringConstants.SEARCH_PATH_INDEXED;

/**
 * A directory-based package loader.
 */

public final class MiDirectoryLoader implements MiLoaderType
{
  private final MiStrings strings;
  private final MiCompilerFactoryType compilers;
  private final List<Path> directories;
  private final HashMap<MiPackageName, MiPackageType> packageCache;
  private final ArrayList<MiPackageName> packageStack;

  /**
   * A directory-based package loader.
   *
   * @param inDirectories The source directories
   * @param inCompilers The compilers
   * @param inStrings   The strings
   */

  public MiDirectoryLoader(
    final MiStrings inStrings,
    final MiCompilerFactoryType inCompilers,
    final List<Path> inDirectories)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.compilers =
      Objects.requireNonNull(inCompilers, "compilers");
    this.directories =
      Objects.requireNonNull(inDirectories, "directories");
    this.packageCache =
      new HashMap<>();
    this.packageStack =
      new ArrayList<>();
  }

  @Override
  public MiPackageType openPackage(
    final MiPackageName name)
    throws MiException
  {
    Objects.requireNonNull(name, "name");

    if (this.packageStack.contains(name)) {
      throw this.errorCircularImport(name);
    }

    this.packageStack.addLast(name);
    try {
      final var existing = this.packageCache.get(name);
      if (existing != null) {
        return existing;
      }

      final var fileName =
        name + ".mpx";

      for (final var directory : this.directories) {
        final var path = directory.resolve(fileName);
        if (!Files.isRegularFile(path)) {
          continue;
        }

        final var compiler =
          this.compilers.create(this);

        final MiCompilerResultType<MiPackageType> compiled;
        try {
          compiled = compiler.compileFile(path);
        } catch (final IOException e) {
          throw this.errorIO(name, e);
        }

        return switch (compiled) {
          case final Failed<MiPackageType> failed -> {
            final var errors =
              new ArrayList<>(failed.errors());
            final var error =
              errors.removeFirst();

            throw new MiException(
              error.message(),
              error.errorCode(),
              error.attributes(),
              error.remediatingAction(),
              errors
            );
          }
          case final Succeeded<MiPackageType> succeeded -> {
            this.packageCache.put(name, succeeded.result());
            yield succeeded.result();
          }
        };
      }

      throw this.errorNoSuchPackage(name);
    } finally {
      this.packageStack.removeLast();
    }
  }

  private MiException errorNoSuchPackage(
    final MiPackageName name)
  {
    final var attributes = new TreeMap<String, String>();
    attributes.put(
      this.strings.format(PACKAGE),
      name.toString()
    );

    for (var index = 0; index < this.directories.size(); ++index) {
      attributes.put(
        this.strings.format(SEARCH_PATH_INDEXED, Integer.valueOf(index)),
        this.directories.get(index).toString()
      );
    }

    return new MiException(
      this.strings.format(ERROR_NO_SUCH_PACKAGE),
      "error-no-such-package",
      attributes,
      Optional.empty(),
      List.of()
    );
  }

  private MiException errorIO(
    final MiPackageName name,
    final IOException e)
  {
    final var attributes = new TreeMap<String, String>();
    attributes.put(
      this.strings.format(PACKAGE),
      name.toString()
    );

    return new MiException(
      this.strings.format(ERROR_IO),
      e,
      "error-io",
      attributes,
      Optional.empty(),
      List.of()
    );
  }

  private MiException errorCircularImport(
    final MiPackageName name)
  {
    final var attributes = new TreeMap<String, String>();
    attributes.put(
      this.strings.format(PACKAGE),
      name.toString()
    );

    for (int index = 0; index < this.packageStack.size(); ++index) {
      attributes.put(
        this.strings.format(ERROR_CIRCULAR_PATH, index),
        this.packageStack.get(index).toString()
      );
    }

    return new MiException(
      this.strings.format(ERROR_CIRCULAR_DEPENDENCY),
      "error-import-circular",
      attributes,
      Optional.empty(),
      List.of()
    );
  }
}
