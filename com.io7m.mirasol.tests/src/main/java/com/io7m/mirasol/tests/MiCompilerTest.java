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

import com.io7m.mirasol.compiler.MiCompilers;
import com.io7m.mirasol.compiler.MiDirectoryLoaders;
import com.io7m.mirasol.compiler.MiStandardPackages;
import com.io7m.mirasol.compiler.api.MiCompilerResultType;
import com.io7m.mirasol.compiler.api.MiCompilerResultType.Failed;
import com.io7m.mirasol.compiler.api.MiCompilerResultType.Succeeded;
import com.io7m.mirasol.compiler.api.MiCompilerType;
import com.io7m.mirasol.core.MiPackageElementType;
import com.io7m.mirasol.core.MiPackageType;
import com.io7m.mirasol.core.MiSimpleName;
import com.io7m.mirasol.loader.api.MiLoaderType;
import com.io7m.mirasol.parser.MiParsers;
import com.io7m.mirasol.parser.MiSerializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MiCompilerTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(MiCompilerTest.class);

  private MiParsers parsers;
  private MiCompilers compilers;
  private MiDirectoryLoaders loaders;
  private MiLoaderType loader;
  private MiCompilerType compiler;
  private Path directory;
  private MiSerializers serializers;

  @BeforeEach
  public void setup(
    final @TempDir Path inDirectory)
  {
    this.directory = inDirectory;

    this.compilers =
      new MiCompilers();
    this.parsers =
      new MiParsers();
    this.serializers =
      new MiSerializers();
    this.loaders =
      new MiDirectoryLoaders(List.of(inDirectory));
    this.loader =
      this.loaders.create();
    this.compiler =
      this.compilers.create(this.loader);
  }

  @Test
  public void testEmpty()
    throws Exception
  {
    final var result =
      this.compiler.compile(
        URI.create("urn:stdin"),
        resource("error-empty.xml")
      );

    dumpResult(result);

    final Failed<MiPackageType> failed =
      (Failed<MiPackageType>) assertInstanceOf(Failed.class, result);

    final var errors = failed.errors();
    assertEquals("parse-error", errors.get(0).errorCode());
  }

  @Test
  public void testCircularImport()
    throws Exception
  {
    Files.copy(
      resource("error-circA.xml"),
      this.directory.resolve("com.io7m.circ_a.mpx")
    );
    Files.copy(
      resource("error-circB.xml"),
      this.directory.resolve("com.io7m.circ_b.mpx")
    );

    final var result =
      this.compiler.compile(
        URI.create("urn:stdin"),
        resource("error-circA.xml")
      );

    dumpResult(result);

    final Failed<MiPackageType> failed =
      (Failed<MiPackageType>) assertInstanceOf(Failed.class, result);

    final var errors = failed.errors();
    final var e0 = errors.get(0);
    assertEquals("error-import-circular", e0.errorCode());
    assertEquals("com.io7m.circ_b", e0.attributes().get("Package"));
    assertEquals("com.io7m.circ_b", e0.attributes().get("Package Path [0]"));
    assertEquals("com.io7m.circ_a", e0.attributes().get("Package Path [1]"));
  }

  @Test
  public void testCircularType0()
    throws Exception
  {
    final var result =
      this.compiler.compile(
        URI.create("urn:stdin"),
        resource("error-type-circ0.xml")
      );

    dumpResult(result);

    final Failed<MiPackageType> failed =
      (Failed<MiPackageType>) assertInstanceOf(Failed.class, result);

    final var errors = failed.errors();
    LOG.debug("Errors: {}", errors);
    final var e0 = errors.get(0);
    assertEquals("error-type-cyclic", e0.errorCode());
  }

  @Test
  public void testCircularType1()
    throws Exception
  {
    final var result =
      this.compiler.compile(
        URI.create("urn:stdin"),
        resource("error-type-circ1.xml")
      );

    dumpResult(result);

    final Failed<MiPackageType> failed =
      (Failed<MiPackageType>) assertInstanceOf(Failed.class, result);

    final var errors = failed.errors();
    final var e0 = errors.get(0);
    assertEquals("error-type-cyclic", e0.errorCode());
  }

  @Test
  public void testATTiny212()
    throws Exception
  {
    Files.copy(
      MiStandardPackages.core().openStream(),
      this.directory.resolve("com.io7m.mirasol.core.mpx")
    );

    final var result =
      this.compiler.compile(
        URI.create("urn:stdin"),
        resource("attiny212.xml")
      );

    dumpResult(result);

    final Succeeded<MiPackageType> success =
      (Succeeded<MiPackageType>) assertInstanceOf(Succeeded.class, result);

    final var pack = success.result();
    assertEquals(7, pack.size());

    assertTrue(pack.type(new MiSimpleName("ATTiny212")).isPresent());
    assertTrue(pack.type(new MiSimpleName("PORT")).isPresent());
    assertTrue(pack.type(new MiSimpleName("VREF")).isPresent());
    assertTrue(pack.type(new MiSimpleName("GPIO")).isPresent());
    assertTrue(pack.type(new MiSimpleName("PINCTRL")).isPresent());
    assertTrue(pack.type(new MiSimpleName("FUSE")).isPresent());

    assertTrue(pack.object(new MiSimpleName("ATTiny212")).isPresent());
    assertTrue(pack.object(new MiSimpleName("PORT")).isPresent());
    assertTrue(pack.object(new MiSimpleName("VREF")).isPresent());
    assertTrue(pack.object(new MiSimpleName("GPIO")).isPresent());
    assertTrue(pack.object(new MiSimpleName("PINCTRL")).isPresent());
    assertTrue(pack.object(new MiSimpleName("FUSE")).isPresent());
    assertTrue(pack.object(new MiSimpleName("ATTiny212Map")).isPresent());

    assertEquals("com.microchip.attiny212", pack.name().toString());

    final var names = List.of(
      "ATTiny212",
      "FUSE",
      "GPIO",
      "PINCTRL",
      "PORT",
      "VREF",
      "ATTiny212Map"
    );

    assertEquals(
      names,
      pack.stream()
        .map(MiPackageElementType::name)
        .map(MiSimpleName::toString)
        .collect(Collectors.toList())
    );

    assertFalse(pack.isEmpty());
    assertTrue(pack.containsAll(pack));

    assertThrows(UnsupportedOperationException.class, () -> {
      pack.addAll(List.of());
    });
    assertThrows(UnsupportedOperationException.class, () -> {
      pack.add(pack.iterator().next());
    });
    assertThrows(UnsupportedOperationException.class, () -> {
      pack.remove(pack.iterator().next());
    });
    assertThrows(UnsupportedOperationException.class, () -> {
      pack.removeAll(List.of());
    });
    assertThrows(UnsupportedOperationException.class, () -> {
      pack.retainAll(List.of());
    });
    assertThrows(UnsupportedOperationException.class, () -> {
      pack.clear();
    });

    this.serializers.serialize(
      URI.create("urn:out"),
      System.out,
      pack
    );
  }

  @Test
  public void testSizeError0()
    throws Exception
  {
    final var result =
      this.compiler.compile(
        URI.create("urn:stdin"),
        resource("error-sizes-0.xml")
      );

    dumpResult(result);

    final Failed<MiPackageType> failed =
      (Failed<MiPackageType>) assertInstanceOf(Failed.class, result);

    final var errors = failed.errors();
    final var e0 = errors.get(0);
    assertEquals("error-type-size-positive", e0.errorCode());
  }

  @Test
  public void testSizes0()
    throws Exception
  {
    final var result =
      this.compiler.compile(
        URI.create("urn:stdin"),
        resource("sizes-0.xml")
      );

    dumpResult(result);

    final Succeeded<MiPackageType> success =
      (Succeeded<MiPackageType>) assertInstanceOf(Succeeded.class, result);

    final var pack = success.result();
    for (var i = 1; i <= 128; ++i) {
      final var type =
        pack.type(new MiSimpleName("U%d".formatted(i)))
          .orElseThrow();

      final int extra;
      if (i % 8 != 0) {
        extra = 1;
      } else {
        extra = 0;
      }

      final var expectedSize = (i / 8) + extra;
      assertEquals(
        expectedSize,
        type.type().size().intValueExact()
      );
    }
  }

  @Test
  public void testSizesBitRangeOverlap0()
    throws Exception
  {
    final var result =
      this.compiler.compile(
        URI.create("urn:stdin"),
        resource("error-bit-range-overlap-0.xml")
      );

    dumpResult(result);

    final Failed<MiPackageType> failed =
      (Failed<MiPackageType>) assertInstanceOf(Failed.class, result);

    final var errors = failed.errors();
    final var e0 = errors.get(0);
    assertEquals("error-bit-field-overlap", e0.errorCode());
  }

  @Test
  public void testSizesBitRangeOverlap1()
    throws Exception
  {
    final var result =
      this.compiler.compile(
        URI.create("urn:stdin"),
        resource("error-bit-range-overlap-1.xml")
      );

    dumpResult(result);

    final Failed<MiPackageType> failed =
      (Failed<MiPackageType>) assertInstanceOf(Failed.class, result);

    final var errors = failed.errors();
    final var e0 = errors.get(0);
    assertEquals("error-bit-field-overlap", e0.errorCode());
  }

  @Test
  public void testSizesBitRangeSizeInsufficient0()
    throws Exception
  {
    final var result =
      this.compiler.compile(
        URI.create("urn:stdin"),
        resource("error-bit-range-size-insufficient-0.xml")
      );

    dumpResult(result);

    final Failed<MiPackageType> failed =
      (Failed<MiPackageType>) assertInstanceOf(Failed.class, result);

    final var errors = failed.errors();
    final var e0 = errors.get(0);
    assertEquals("error-bit-field-size-insufficient", e0.errorCode());
  }

  @Test
  public void testSizesBitRangeSizeInsufficient1()
    throws Exception
  {
    final var result =
      this.compiler.compile(
        URI.create("urn:stdin"),
        resource("error-bit-range-size-insufficient-1.xml")
      );

    dumpResult(result);

    final Failed<MiPackageType> failed =
      (Failed<MiPackageType>) assertInstanceOf(Failed.class, result);

    final var errors = failed.errors();
    final var e0 = errors.get(0);
    assertEquals("error-bit-field-size-insufficient", e0.errorCode());
  }

  @Test
  public void testSizesFieldOverlap0()
    throws Exception
  {
    final var result =
      this.compiler.compile(
        URI.create("urn:stdin"),
        resource("error-field-overlap-0.xml")
      );

    dumpResult(result);

    final Failed<MiPackageType> failed =
      (Failed<MiPackageType>) assertInstanceOf(Failed.class, result);

    final var errors = failed.errors();
    final var e0 = errors.get(0);
    assertEquals("error-field-overlap", e0.errorCode());
  }

  @Test
  public void testSizesFieldOverlap1()
    throws Exception
  {
    final var result =
      this.compiler.compile(
        URI.create("urn:stdin"),
        resource("error-field-overlap-1.xml")
      );

    dumpResult(result);

    final Failed<MiPackageType> failed =
      (Failed<MiPackageType>) assertInstanceOf(Failed.class, result);

    final var errors = failed.errors();
    final var e0 = errors.get(0);
    assertEquals("error-field-overlap", e0.errorCode());
  }

  @Test
  public void testSizesFieldOverlap2()
    throws Exception
  {
    final var result =
      this.compiler.compile(
        URI.create("urn:stdin"),
        resource("error-field-overlap-2.xml")
      );

    dumpResult(result);

    final Failed<MiPackageType> failed =
      (Failed<MiPackageType>) assertInstanceOf(Failed.class, result);

    final var errors = failed.errors();
    final var e0 = errors.get(0);
    assertEquals("error-field-overlap", e0.errorCode());
  }

  @Test
  public void testSizesFieldOverlap3()
    throws Exception
  {
    final var result =
      this.compiler.compile(
        URI.create("urn:stdin"),
        resource("error-field-overlap-3.xml")
      );

    dumpResult(result);

    final Failed<MiPackageType> failed =
      (Failed<MiPackageType>) assertInstanceOf(Failed.class, result);

    final var errors = failed.errors();
    final var e0 = errors.get(0);
    assertEquals("error-field-overlap", e0.errorCode());
  }

  @Test
  public void testSizeAssertionFailed0()
    throws Exception
  {
    final var result =
      this.compiler.compile(
        URI.create("urn:stdin"),
        resource("error-size-assertion-0.xml")
      );

    dumpResult(result);

    final Failed<MiPackageType> failed =
      (Failed<MiPackageType>) assertInstanceOf(Failed.class, result);

    final var errors = failed.errors();
    final var e0 = errors.get(0);
    assertEquals("error-size-assertion-failed", e0.errorCode());
  }

  @Test
  public void testSizeAssertionFailed1()
    throws Exception
  {
    final var result =
      this.compiler.compile(
        URI.create("urn:stdin"),
        resource("error-size-assertion-1.xml")
      );

    dumpResult(result);

    final Failed<MiPackageType> failed =
      (Failed<MiPackageType>) assertInstanceOf(Failed.class, result);

    final var errors = failed.errors();
    final var e0 = errors.get(0);
    assertEquals("error-size-assertion-failed", e0.errorCode());
  }

  @Test
  public void testMapReferencesMap0()
    throws Exception
  {
    final var result =
      this.compiler.compile(
        URI.create("urn:stdin"),
        resource("error-type-ref-map-0.xml")
      );

    dumpResult(result);

    final Failed<MiPackageType> failed =
      (Failed<MiPackageType>) assertInstanceOf(Failed.class, result);

    final var errors = failed.errors();
    final var e0 = errors.get(0);
    assertEquals("error-type-reference-map", e0.errorCode());
  }

  private static void dumpResult(
    final MiCompilerResultType<MiPackageType> result)
  {
    switch (result) {
      case final Failed<MiPackageType> failed -> {
        for (final var error : failed.errors()) {
          LOG.debug("{}", error);
        }
      }
      case final Succeeded<MiPackageType> succeeded -> {

      }
    }
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
}
