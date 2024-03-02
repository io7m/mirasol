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


package com.io7m.mirasol.extractor.cflat.internal;

import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.mirasol.core.MiBitFieldType;
import com.io7m.mirasol.core.MiBitRangeType;
import com.io7m.mirasol.core.MiFieldType;
import com.io7m.mirasol.core.MiMapType;
import com.io7m.mirasol.core.MiPackageType;
import com.io7m.mirasol.core.MiScalarType;
import com.io7m.mirasol.core.MiSimpleName;
import com.io7m.mirasol.core.MiSizeOctets;
import com.io7m.mirasol.core.MiStructureType;
import com.io7m.mirasol.core.MiTypeType;
import com.io7m.mirasol.core.MiTypedFieldType;
import com.io7m.mirasol.extractor.api.MiExtractorConfiguration;
import com.io7m.mirasol.extractor.api.MiExtractorException;
import com.io7m.mirasol.extractor.api.MiExtractorType;
import com.io7m.mirasol.strings.MiStrings;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.io7m.mirasol.core.MiScalarKindStandard.INTEGER_SIGNED;
import static com.io7m.mirasol.core.MiScalarKindStandard.INTEGER_UNSIGNED;
import static com.io7m.mirasol.strings.MiStringConstants.ERROR_IO;
import static com.io7m.mirasol.strings.MiStringConstants.ERROR_UNSUPPORTED_BIT_FIELD_TYPE;
import static com.io7m.mirasol.strings.MiStringConstants.ERROR_UNSUPPORTED_SCALAR_TYPE;
import static com.io7m.mirasol.strings.MiStringConstants.FIELD;
import static com.io7m.mirasol.strings.MiStringConstants.PACKAGE;
import static com.io7m.mirasol.strings.MiStringConstants.TYPE;

/**
 * An extractor for C/C++.
 */

public final class MiExtractorCFlat
  implements MiExtractorType
{
  private static final OpenOption[] OPEN_OPTIONS = {
    StandardOpenOption.CREATE,
    StandardOpenOption.WRITE,
    StandardOpenOption.TRUNCATE_EXISTING,
  };

  private final MiExtractorConfiguration configuration;
  private final MiStrings strings;
  private final ArrayList<MiDefineType> defines;
  private BigInteger paddingIndex;

  /**
   * An extractor for C/C++.
   *
   * @param inConfiguration The configuration
   */

  public MiExtractorCFlat(
    final MiExtractorConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.paddingIndex =
      BigInteger.ZERO;
    this.strings =
      MiStrings.create(Locale.getDefault());
    this.defines =
      new ArrayList<MiDefineType>();
  }

  @Override
  public void execute()
    throws MiExtractorException
  {
    final var exceptionTracker =
      new ExceptionTracker<MiExtractorException>();

    for (final var pack : this.configuration.packageList()) {
      try {
        this.executePackage(pack);
      } catch (final MiExtractorException e) {
        exceptionTracker.addException(e);
      }
    }

    exceptionTracker.throwIfNecessary();
  }

  private void executePackage(
    final MiPackageType pack)
    throws MiExtractorException
  {
    try {
      final var fileName =
        fileNameOf(pack);
      final var outputDirectory =
        this.configuration.outputDirectory();
      final var path =
        outputDirectory.resolve(fileName);

      Files.createDirectories(outputDirectory);
      try (var writer = Files.newBufferedWriter(path, OPEN_OPTIONS)) {
        this.executePackageFile(pack, writer);
      }
    } catch (final IOException e) {
      throw this.errorIO(pack, e);
    }
  }

  private MiExtractorException errorIO(
    final MiPackageType pack,
    final IOException e)
  {
    final var attributes = new TreeMap<String, String>();
    attributes.put(
      this.strings.format(PACKAGE),
      pack.name().toString()
    );

    return new MiExtractorException(
      this.strings.format(ERROR_IO),
      e,
      "error-io",
      attributes,
      Optional.empty(),
      List.of()
    );
  }

  private void executePackageFile(
    final MiPackageType pack,
    final BufferedWriter writer)
    throws IOException, MiExtractorException
  {
    final var guardName = guardNameOf(pack);
    writer.append("#ifndef ");
    writer.append(guardName);
    writer.append('\n');

    writer.append("#define ");
    writer.append(guardName);
    writer.append('\n');
    writer.append('\n');

    writer.append("// Automatically generated. DO NOT EDIT.\n");
    writer.append('\n');

    writer.append("#include <stdint.h>\n");
    writer.append("#include <assert.h>\n");
    writer.append('\n');

    this.executePackageFileMaps(pack, writer);

    writer.append("#endif // ");
    writer.append(guardName);
    writer.append('\n');
  }

  private void executePackageFileMaps(
    final MiPackageType pack,
    final BufferedWriter writer)
    throws IOException, MiExtractorException
  {
    if (!pack.maps().isEmpty()) {
      final var pathStart = startPath(pack);

      for (final var map : pack.maps()) {
        this.executePackageFileMap(
          pack,
          writer,
          pathStart.with(new MiNamedOffset(map.name(), map.offset())),
          map
        );
      }
      writer.append('\n');
    }
  }

  private static MiNamedOffsetPath startPath(
    final MiPackageType pack)
  {
    return new MiNamedOffsetPath(
      pack.name()
        .value()
        .segments()
        .stream()
        .map(x -> x.toUpperCase(Locale.ROOT))
        .map(x -> new MiNamedOffset(new MiSimpleName(x), BigInteger.ZERO))
        .collect(Collectors.toList())
    );
  }

  private void executePackageFileMap(
    final MiPackageType pack,
    final BufferedWriter writer,
    final MiNamedOffsetPath path,
    final MiMapType map)
    throws IOException, MiExtractorException
  {
    this.defines.clear();
    this.defines.add(new MiDefineAddress(path.toCName(), "void", BigInteger.ZERO));

    final var type = map.type().type();
    this.executePackageFileType(pack, path, type);

    var defineMax = 0;
    for (final var define : this.defines) {
      defineMax = Math.max(define.name().length() + 2, defineMax);
    }

    for (final var define : this.defines) {
      writeDefine(writer, defineMax, define);
    }
  }

  private static void writeDefine(
    final BufferedWriter writer,
    final int defineMax,
    final MiDefineType define)
    throws IOException
  {
    switch (define) {
      case final MiDefineAddress address -> {
        final var pad =
          defineMax - (define.name().length());

        writer.append(
          "#define %s%s((%s * const) 0x%s)\n".formatted(
            address.name(),
            " ".repeat(pad),
            address.type(),
            address.offset().toString(16)
          )
        );
      }

      case final MiDefineInteger integer -> {
        final var pad =
          defineMax - (define.name().length());

        writer.append(
          "#define %s%s %s\n".formatted(
            integer.name(),
            " ".repeat(pad),
            switch (integer.radix) {
              case 10 -> integer.value().toString(10);
              case 2 -> "0b" + integer.value().toString(2);
              case 16 -> "0x" + integer.value().toString(16);
              default -> throw new UnimplementedCodeException();
            }
          )
        );
      }
    }
  }

  private void executePackageFileType(
    final MiPackageType pack,
    final MiNamedOffsetPath path,
    final MiTypeType type)
    throws IOException, MiExtractorException
  {
    switch (type) {
      case final MiScalarType scalar -> {
        this.executePackageFileTypeScalar(
          pack,
          path,
          scalar
        );
      }

      case final MiStructureType structure -> {
        this.executePackageFileTypeStructure(
          pack,
          path,
          structure
        );
      }
    }
  }

  private void executePackageFileTypeScalar(
    final MiPackageType pack,
    final MiNamedOffsetPath path,
    final MiScalarType scalar)
    throws MiExtractorException
  {
    final var offsetSum =
      path.values()
        .stream()
        .map(MiNamedOffset::offset)
        .reduce(BigInteger.ZERO, BigInteger::add);

    this.defines.add(
      new MiDefineAddress(
        path.toCName(),
        this.cTypeOf(pack, scalar),
        offsetSum
      )
    );
  }

  private String cTypeOf(
    final MiPackageType pack,
    final MiScalarType scalar)
    throws MiExtractorException
  {
    final var size = scalar.size();
    switch (scalar.kind()) {
      case INTEGER_SIGNED -> {
        if (Objects.equals(size, MiSizeOctets.of(1L))) {
          return "int8_t";
        } else if (Objects.equals(size, MiSizeOctets.of(2L))) {
          return "int16_t";
        } else if (Objects.equals(size, MiSizeOctets.of(4L))) {
          return "int32_t";
        } else if (Objects.equals(size, MiSizeOctets.of(8L))) {
          return "int64_t";
        } else {
          throw this.errorUnsupportedScalarType(pack, scalar);
        }
      }
      case INTEGER_UNSIGNED -> {
        if (Objects.equals(size, MiSizeOctets.of(1L))) {
          return "uint8_t";
        } else if (Objects.equals(size, MiSizeOctets.of(2L))) {
          return "uint16_t";
        } else if (Objects.equals(size, MiSizeOctets.of(4L))) {
          return "uint32_t";
        } else if (Objects.equals(size, MiSizeOctets.of(8L))) {
          return "uint64_t";
        } else {
          throw this.errorUnsupportedScalarType(pack, scalar);
        }
      }
      default -> {
        throw this.errorUnsupportedScalarType(pack, scalar);
      }
    }
  }

  private void executePackageFileTypeStructure(
    final MiPackageType pack,
    final MiNamedOffsetPath path,
    final MiStructureType structure)
    throws IOException, MiExtractorException
  {
    for (final var field : structure.fields()) {
      this.executePackageFileTypeStructureField(
        pack,
        path.with(new MiNamedOffset(
          field.name(),
          field.offset()
        )),
        structure,
        field
      );
    }
  }

  private void executePackageFileTypeStructureField(
    final MiPackageType pack,
    final MiNamedOffsetPath path,
    final MiStructureType structure,
    final MiFieldType field)
    throws IOException, MiExtractorException
  {
    switch (field) {
      case final MiBitFieldType bitField -> {
        this.executePackageFileTypeStructureFieldBit(
          pack,
          path,
          structure,
          bitField
        );
      }
      case final MiTypedFieldType typedField -> {
        this.executePackageFileTypeStructureFieldTyped(
          pack,
          path,
          structure,
          typedField
        );
      }
    }
  }

  private void executePackageFileTypeStructureFieldBit(
    final MiPackageType pack,
    final MiNamedOffsetPath path,
    final MiStructureType structure,
    final MiBitFieldType bitField)
    throws MiExtractorException
  {
    final var size = bitField.size();

    final String cType;
    if (Objects.equals(size, MiSizeOctets.of(1L))) {
      cType = "uint8_t";
    } else if (Objects.equals(size, MiSizeOctets.of(2L))) {
      cType = "uint16_t";
    } else if (Objects.equals(size, MiSizeOctets.of(4L))) {
      cType = "uint32_t";
    } else if (Objects.equals(size, MiSizeOctets.of(8L))) {
      cType = "uint64_t";
    } else {
      throw this.errorUnsupportedBitFieldType(pack, structure, bitField);
    }

    final var offsetSum =
      path.values()
        .stream()
        .map(MiNamedOffset::offset)
        .reduce(BigInteger.ZERO, BigInteger::add);

    this.defines.add(
      new MiDefineAddress(
        path.toCName(),
        cType,
        offsetSum
      )
    );

    for (final var range : bitField.ranges()) {
      this.executePackageFileTypeStructureFieldBitRange(
        pack,
        path.with(new MiNamedOffset(
          range.name(),
          BigInteger.ZERO
        )),
        range
      );
    }
  }

  private void executePackageFileTypeStructureFieldBitRange(
    final MiPackageType pack,
    final MiNamedOffsetPath path,
    final MiBitRangeType range)
  {
    this.defines.add(
      new MiDefineInteger(
        path.toCName() + "__OFFSET",
        range.range().lower(),
        10
      )
    );

    final var bitCount =
      range.range()
        .upper()
        .subtract(range.range().lower())
        .add(BigInteger.ONE);

    final var mask =
      BigInteger.valueOf(2L)
        .pow(bitCount.intValueExact())
        .subtract(BigInteger.ONE);

    this.defines.add(
      new MiDefineInteger(
        path.toCName() + "__MASK",
        mask,
        2
      )
    );
  }

  private void executePackageFileTypeStructureFieldTyped(
    final MiPackageType pack,
    final MiNamedOffsetPath path,
    final MiStructureType structure,
    final MiTypedFieldType typedField)
    throws IOException, MiExtractorException
  {
    this.executePackageFileType(
      pack,
      path,
      typedField.type().type()
    );
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

  private MiExtractorException errorUnsupportedScalarType(
    final MiPackageType pack,
    final MiScalarType scalar)
  {
    final var attributes = new TreeMap<String, String>();

    attributes.put(
      this.strings.format(PACKAGE),
      pack.name().toString()
    );
    attributes.put(
      this.strings.format(TYPE),
      scalar.name().value()
    );

    return new MiExtractorException(
      this.strings.format(ERROR_UNSUPPORTED_SCALAR_TYPE),
      "error-unsupported-scalar-type",
      attributes,
      Optional.empty(),
      List.of()
    );
  }


  private MiExtractorException errorUnsupportedBitFieldType(
    final MiPackageType pack,
    final MiStructureType structure,
    final MiBitFieldType bitField)
  {
    final var attributes = new TreeMap<String, String>();

    attributes.put(
      this.strings.format(PACKAGE),
      pack.name().toString()
    );
    attributes.put(
      this.strings.format(TYPE),
      structure.name().value()
    );
    attributes.put(
      this.strings.format(FIELD),
      bitField.name().toString()
    );

    return new MiExtractorException(
      this.strings.format(ERROR_UNSUPPORTED_BIT_FIELD_TYPE),
      "error-unsupported-bit-field-type",
      attributes,
      Optional.empty(),
      List.of()
    );
  }

  private sealed interface MiDefineType {
    String name();
  }

  private record MiDefineAddress(
    String name,
    String type,
    BigInteger offset)
    implements MiDefineType
  {

  }

  private record MiDefineInteger(
    String name,
    BigInteger value,
    int radix)
    implements MiDefineType
  {

  }
}
