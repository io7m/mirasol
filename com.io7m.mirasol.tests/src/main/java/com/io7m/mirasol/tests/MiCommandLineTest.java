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


package com.io7m.mirasol.tests;

import com.io7m.mirasol.cmdline.MiMain;
import com.io7m.mirasol.compiler.MiStandardPackages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class MiCommandLineTest
{
  private Path directory;

  @BeforeEach
  public void setup(
    final @TempDir Path directory)
    throws IOException
  {
    this.directory = directory;

    Files.copy(
      MiStandardPackages.core().openStream(),
      directory.resolve("com.io7m.mirasol.core.mpx")
    );
  }

  @Test
  public void testHelp()
  {
    final var main = new MiMain(new String[0]);
    final var app = main.application();

    for (final var cmd : app.commandTree().keySet()) {
      final var main2 = new MiMain(new String[]{
        "help",
        cmd,
      });
      main2.run();
      assertEquals(0, main2.exitCode());
    }
  }

  @Test
  public void testExtractors()
  {
    final var main = new MiMain(new String[]{"extractors"});
    main.run();
    assertEquals(0, main.exitCode());
  }

  @TestFactory
  public Stream<DynamicTest> testCompileFailures()
  {
    return Stream.of(
      "error-bit-range-overlap-0.xml",
      "error-bit-range-overlap-1.xml",
      "error-bit-range-size-insufficient-0.xml",
      "error-bit-range-size-insufficient-1.xml",
      "error-circA.xml",
      "error-circB.xml",
      "error-empty.xml",
      "error-field-overlap-0.xml",
      "error-field-overlap-1.xml",
      "error-field-overlap-2.xml",
      "error-field-overlap-3.xml",
      "error-size-assertion-0.xml",
      "error-size-assertion-1.xml",
      "error-sizes-0.xml",
      "error-type-circ0.xml",
      "error-type-circ1.xml",
      "error-type-ref-map-0.xml"
    ).map(file -> {
      return DynamicTest.dynamicTest(
        "testCompileFailure_%s".formatted(file),
        () -> {
          this.compileFail(file);
        });
    });
  }

  @TestFactory
  public Stream<DynamicTest> testCompileSuccess()
  {
    return Stream.of(
      "attiny212.xml",
      "sizes-0.xml"
    ).map(file -> {
      return DynamicTest.dynamicTest(
        "testCompileSuccess_%s".formatted(file),
        () -> {
          this.compileSuccess(file);
        });
    });
  }

  private static InputStream resource(
    final String name)
    throws IOException
  {
    final var path =
      "/com/io7m/mirasol/tests/%s".formatted(name);
    final var url =
      MiCompilerTest.class.getResource(path);

    return url.openStream();
  }

  private void compileFail(
    final String file)
    throws IOException
  {
    final var path = this.directory.resolve("file.xml");
    Files.deleteIfExists(path);

    try (var stream = resource(file)) {
      Files.copy(stream, path);
    }

    final var main = new MiMain(new String[]{
      "compile",
      "--file", path.toString(),
      "--package-directory",
      this.directory.toString(),
    });
    main.run();
    assertEquals(1, main.exitCode());
  }

  private void compileSuccess(
    final String file)
    throws IOException
  {
    final var path = this.directory.resolve("file.xml");
    Files.deleteIfExists(path);

    try (var stream = resource(file)) {
      Files.copy(stream, path);
    }

    final var main = new MiMain(new String[]{
      "compile",
      "--file", path.toString(),
      "--package-directory",
      this.directory.toString(),
    });
    main.run();
    assertEquals(0, main.exitCode());
  }
}
