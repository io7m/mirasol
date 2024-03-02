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

import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.mirasol.core.MiBitFieldType;
import com.io7m.mirasol.core.MiBitRangeType;
import com.io7m.mirasol.core.MiFieldType;
import com.io7m.mirasol.core.MiMapType;
import com.io7m.mirasol.core.MiPackageName;
import com.io7m.mirasol.core.MiPackageReference;
import com.io7m.mirasol.core.MiPackageType;
import com.io7m.mirasol.core.MiScalarType;
import com.io7m.mirasol.core.MiSimpleName;
import com.io7m.mirasol.core.MiSizeOctets;
import com.io7m.mirasol.core.MiStructureType;
import com.io7m.mirasol.core.MiTypeReference;
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.io7m.mirasol.core.MiScalarKindStandard.INTEGER_SIGNED;
import static com.io7m.mirasol.core.MiScalarKindStandard.INTEGER_UNSIGNED;
import static com.io7m.mirasol.strings.MiStringConstants.CPP_OFFSET_OF;
import static com.io7m.mirasol.strings.MiStringConstants.CPP_SIZE_OF;
import static com.io7m.mirasol.strings.MiStringConstants.ERROR_IO;
import static com.io7m.mirasol.strings.MiStringConstants.ERROR_UNSUPPORTED_BIT_FIELD_TYPE;
import static com.io7m.mirasol.strings.MiStringConstants.ERROR_UNSUPPORTED_SCALAR_TYPE;
import static com.io7m.mirasol.strings.MiStringConstants.FIELD;
import static com.io7m.mirasol.strings.MiStringConstants.PACKAGE;
import static com.io7m.mirasol.strings.MiStringConstants.TYPE;

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
  private final MiStrings strings;
  private BigInteger paddingIndex;
  private BufferedWriter writer;
  private MiPackageType packageNow;

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
    this.paddingIndex =
      BigInteger.ZERO;
    this.strings =
      MiStrings.create(Locale.getDefault());
  }

  @Override
  public void execute()
    throws MiExtractorException
  {
    final var exceptionTracker =
      new ExceptionTracker<MiExtractorException>();

    for (final var pack : this.configuration.packageList()) {
      try {
        this.packageNow = pack;
        this.executePackage();
      } catch (final MiExtractorException e) {
        exceptionTracker.addException(e);
      }
    }

    exceptionTracker.throwIfNecessary();
  }

  private void executePackage()
    throws MiExtractorException
  {
    try {
      final var fileName =
        fileNameOf(this.packageNow);
      final var outputDirectory =
        this.configuration.outputDirectory();
      final var path =
        outputDirectory.resolve(fileName);

      Files.createDirectories(outputDirectory);
      this.writer = Files.newBufferedWriter(path, OPEN_OPTIONS);
      try (var w = this.writer) {
        this.writePackage();
      }
    } catch (final IOException e) {
      throw this.errorIO(this.packageNow, e);
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

  private void writePackage()
    throws IOException, MiExtractorException
  {
    final var guardName = guardNameOf(this.packageNow);
    this.writer.append("#ifndef ");
    this.writer.append(guardName);
    this.writer.append('\n');

    this.writer.append("#define ");
    this.writer.append(guardName);
    this.writer.append('\n');
    this.writer.append('\n');

    this.writer.append("// Automatically generated. DO NOT EDIT.\n");
    this.writer.append("// Extractor: com.io7m.mirasol.extractor.cpp\n");
    this.writer.append("// Package: ");
    this.writer.append(this.packageNow.name().toString());
    this.writer.append('\n');
    this.writer.append('\n');

    this.writer.append("#include <stdint.h>\n");
    this.writer.append("#include <stddef.h>\n");
    this.writer.append("#include <assert.h>\n");
    this.writer.append('\n');

    final var pathStart = this.startPath();
    this.writePackageImports();
    this.writePackageTypes(pathStart);
    this.writePackageMaps(pathStart);

    this.writer.append("#endif // ");
    this.writer.append(guardName);
    this.writer.append('\n');
  }

  private MiNamedOffsetPath startPath()
  {
    return new MiNamedOffsetPath(
      this.packageNow.name()
        .value()
        .segments()
        .stream()
        .map(x -> new MiNamedOffset(new MiSimpleName(x), BigInteger.ZERO))
        .collect(Collectors.toList())
    );
  }

  private void writePackageMaps(
    final MiNamedOffsetPath path)
    throws IOException
  {
    if (!this.packageNow.maps().isEmpty()) {
      for (final var map : this.packageNow.maps()) {
        this.writeMap(
          path.with(new MiNamedOffset(map.name(), map.offset())),
          map
        );
      }
      this.writer.append('\n');
    }
  }

  private void writePackageTypes(
    final MiNamedOffsetPath path)
    throws IOException, MiExtractorException
  {
    final var typesOrdered = this.packageNow.typesTopological();
    if (!typesOrdered.isEmpty()) {
      for (final var type : typesOrdered) {
        this.writeType(
          path.with(new MiNamedOffset(type.name(), BigInteger.ZERO)),
          type
        );
      }
      this.writer.append('\n');
    }
  }

  private void writePackageImports()
    throws IOException
  {
    if (!this.packageNow.imports().isEmpty()) {
      for (final var importE : this.packageNow.imports()) {
        this.writeImport(importE);
      }
      this.writer.append('\n');
    }
  }

  private void writeImport(
    final MiPackageReference importE)
    throws IOException
  {
    this.writer.append(
      "#include \"%s\"\n"
        .formatted(headerNameOf(importE.packageName()))
    );
  }

  private static String headerNameOf(
    final MiPackageName packageName)
  {
    final var packName =
      packageName.toString()
        .replace('.', '_');

    return String.format("%s.h", packName);
  }

  private void writeType(
    final MiNamedOffsetPath path,
    final MiTypeType type)
    throws IOException, MiExtractorException
  {
    switch (type) {
      case final MiScalarType scalar -> {
        this.writeScalar(path, scalar);
      }
      case final MiStructureType structure -> {
        this.writeStructure(path, structure);
      }
    }
  }

  private void writeStructure(
    final MiNamedOffsetPath path,
    final MiStructureType structure)
    throws IOException, MiExtractorException
  {
    final var typeName = path.toCName();
    this.writer.append("typedef struct {\n");

    var offsetPrevious = BigInteger.ZERO;

    for (final var field : structure.fields()) {
      this.writeStructureField(
        path.with(new MiNamedOffset(field.name(), field.offset())),
        structure,
        field,
        offsetPrevious
      );
      offsetPrevious = field.offset().add(field.size().value());
    }

    this.writer.append("} ");
    this.writer.append(typeName);
    this.writer.append(";\n");
    this.writer.append("\n");

    final var bitFieldCount =
      structure.fields()
        .stream()
        .filter(p -> p instanceof MiBitFieldType)
        .count();

    if (bitFieldCount > 0L) {
      this.writer.append("// Left shift and mask values for bit fields.\n");
      for (final var field : structure.fields()) {
        switch (field) {
          case final MiBitFieldType bitField -> {
            this.writeStructureBitFieldConstants(
              path.with(new MiNamedOffset(field.name(), field.offset())),
              bitField
            );
          }
          case final MiTypedFieldType ignored -> {

          }
        }
      }
      this.writer.append("\n");
    }

    this.writer.append("// Size and offset assertions.\n");
    this.writer.append(
      "static_assert(sizeof(%s) == %s, %s);\n".formatted(
        typeName,
        structure.size(),
        '"' + this.strings.format(CPP_SIZE_OF, typeName, structure.size()) + '"'
      )
    );

    for (final var field : structure.fields()) {
      final var name = field.name();
      final var offset = field.offset();
      this.writer.append(
        "static_assert(offsetof(%s, %s) == %s, %s);\n".formatted(
          typeName,
          name,
          offset,
          '"' + this.strings.format(CPP_OFFSET_OF, name, typeName, offset) + '"'
        )
      );
    }

    this.writer.append("\n");
  }

  private void writeStructureBitFieldConstants(
    final MiNamedOffsetPath path,
    final MiBitFieldType bitField)
    throws IOException
  {
    for (final var range : bitField.ranges()) {
      this.writeStructureBitFieldConstantsRange(
        path.with(new MiNamedOffset(range.name(), BigInteger.ZERO)),
        range
      );
    }
  }

  private void writeStructureBitFieldConstantsRange(
    final MiNamedOffsetPath path,
    final MiBitRangeType range)
    throws IOException
  {
    final var bitCount =
      range.range()
        .upper()
        .subtract(range.range().lower())
        .add(BigInteger.ONE);

    final var mask =
      BigInteger.valueOf(2L)
        .pow(bitCount.intValueExact())
        .subtract(BigInteger.ONE);

    this.writer.append(
      "#define %s %s\n".formatted(
        path.toCName() + "__SHIFT",
        range.range().lower()
      )
    );

    this.writer.append(
      "#define %s %s\n".formatted(
        path.toCName() + "__MASK",
        "0b" + mask.toString(2)
      )
    );
  }

  private void writeStructureField(
    final MiNamedOffsetPath path,
    final MiStructureType structure,
    final MiFieldType field,
    final BigInteger offsetPrevious)
    throws IOException, MiExtractorException
  {
    if (!offsetPrevious.equals(field.offset())) {
      this.insertPadding(field.offset().subtract(offsetPrevious));
    }

    switch (field) {
      case final MiBitFieldType bitField -> {
        this.writeStructureFieldBit(structure, bitField);
      }
      case final MiTypedFieldType typedField -> {
        this.writeStructureFieldTyped(typedField);
      }
    }
  }

  private void insertPadding(
    final BigInteger count)
    throws IOException
  {
    this.writer.append("  uint8_t padding");
    this.writer.append(this.paddingIndex.toString());
    this.writer.append("[");
    this.writer.append(count.toString());
    this.writer.append("];\n");
    this.paddingIndex = this.paddingIndex.add(BigInteger.ONE);
  }

  private void writeStructureFieldTyped(
    final MiTypedFieldType typedField)
    throws IOException
  {
    this.writer.append("  ");
    this.writer.append(typeNameOf(typedField.type()));
    this.writer.append(" ");
    this.writer.append(typedField.name().value());
    this.writer.append(";\n");
  }

  private void writeStructureFieldBit(
    final MiStructureType structure,
    final MiBitFieldType bitField)
    throws IOException, MiExtractorException
  {
    this.writer.append("  ");

    final var size = bitField.size();
    if (Objects.equals(size, MiSizeOctets.of(1L))) {
      this.writer.append("uint8_t");
    } else if (Objects.equals(size, MiSizeOctets.of(2L))) {
      this.writer.append("uint16_t");
    } else if (Objects.equals(size, MiSizeOctets.of(4L))) {
      this.writer.append("uint32_t");
    } else if (Objects.equals(size, MiSizeOctets.of(8L))) {
      this.writer.append("uint64_t");
    } else {
      throw this.errorUnsupportedBitFieldType(structure, bitField);
    }

    this.writer.append(" ");
    this.writer.append(bitField.name().value());
    this.writer.append(";\n");
  }

  private static String typeNameOf(
    final MiTypeReference type)
  {
    final var packName =
      type.packageName()
        .toString()
        .replace('.', '_');

    return String.format("%s_%s", packName, type.type().name());
  }

  private void writeScalar(
    final MiNamedOffsetPath path,
    final MiScalarType scalar)
    throws IOException, MiExtractorException
  {
    this.writer.append("typedef ");

    final var size = scalar.size();
    switch (scalar.kind()) {
      case INTEGER_SIGNED -> {
        if (Objects.equals(size, MiSizeOctets.of(1L))) {
          this.writer.append("int8_t");
        } else if (Objects.equals(size, MiSizeOctets.of(2L))) {
          this.writer.append("int16_t");
        } else if (Objects.equals(size, MiSizeOctets.of(4L))) {
          this.writer.append("int32_t");
        } else if (Objects.equals(size, MiSizeOctets.of(8L))) {
          this.writer.append("int64_t");
        } else {
          throw this.errorUnsupportedScalarType(scalar);
        }
      }
      case INTEGER_UNSIGNED -> {
        if (Objects.equals(size, MiSizeOctets.of(1L))) {
          this.writer.append("uint8_t");
        } else if (Objects.equals(size, MiSizeOctets.of(2L))) {
          this.writer.append("uint16_t");
        } else if (Objects.equals(size, MiSizeOctets.of(4L))) {
          this.writer.append("uint32_t");
        } else if (Objects.equals(size, MiSizeOctets.of(8L))) {
          this.writer.append("uint64_t");
        } else {
          throw this.errorUnsupportedScalarType(scalar);
        }
      }
      default -> {
        throw this.errorUnsupportedScalarType(scalar);
      }
    }

    this.writer.append(" ");
    this.writer.append(path.toCName());
    this.writer.append(";\n");
  }

  private void writeMap(
    final MiNamedOffsetPath path,
    final MiMapType map)
    throws IOException
  {
    this.writer.append(
      String.format(
        "%s * const %s = (%s * const) %s;\n",
        typeNameOf(map.type()),
        path.toCName(),
        typeNameOf(map.type()),
        map.offset()
      )
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
    final MiScalarType scalar)
  {
    final var attributes = new TreeMap<String, String>();

    attributes.put(
      this.strings.format(PACKAGE),
      this.packageNow.name().toString()
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
    final MiStructureType structure,
    final MiBitFieldType bitField)
  {
    final var attributes = new TreeMap<String, String>();

    attributes.put(
      this.strings.format(PACKAGE),
      this.packageNow.name().toString()
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
}
