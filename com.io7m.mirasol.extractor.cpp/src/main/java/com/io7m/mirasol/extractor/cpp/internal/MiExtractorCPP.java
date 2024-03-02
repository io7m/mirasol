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

    executePackageFileImports(pack, writer);
    this.executePackageFileTypes(pack, writer);
    executePackageFileMaps(pack, writer);

    writer.append("#endif // ");
    writer.append(guardName);
    writer.append('\n');
  }

  private static void executePackageFileMaps(
    final MiPackageType pack,
    final BufferedWriter writer)
    throws IOException
  {
    if (!pack.maps().isEmpty()) {
      for (final var map : pack.maps()) {
        executePackageFileMap(pack, writer, map);
      }
      writer.append('\n');
    }
  }

  private void executePackageFileTypes(
    final MiPackageType pack,
    final BufferedWriter writer)
    throws IOException, MiExtractorException
  {
    final var typesOrdered = pack.typesTopological();
    if (!typesOrdered.isEmpty()) {
      for (final var type : typesOrdered) {
        this.executePackageFileType(pack, writer, type);
      }
      writer.append('\n');
    }
  }

  private static void executePackageFileImports(
    final MiPackageType pack,
    final BufferedWriter writer)
    throws IOException
  {
    if (!pack.imports().isEmpty()) {
      for (final var importE : pack.imports()) {
        executePackageFileImport(writer, importE);
      }
      writer.append('\n');
    }
  }

  private static void executePackageFileImport(
    final BufferedWriter writer,
    final MiPackageReference importE)
    throws IOException
  {
    writer.append(
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

  private void executePackageFileType(
    final MiPackageType pack,
    final BufferedWriter writer,
    final MiTypeType type)
    throws IOException, MiExtractorException
  {
    switch (type) {
      case final MiScalarType sc -> {
        this.executePackageFileTypeScalar(pack, writer, sc);
      }
      case final MiStructureType structure -> {
        this.executePackageFileTypeStruct(pack, writer, structure);
      }
    }
  }

  private void executePackageFileTypeStruct(
    final MiPackageType pack,
    final BufferedWriter writer,
    final MiStructureType structure)
    throws IOException, MiExtractorException
  {
    final var typeName =
      typeNameOf(pack, structure.name());

    writer.append("typedef struct {\n");

    var offsetPrevious = BigInteger.ZERO;

    for (final var field : structure.fields()) {
      this.executePackageFileTypeStructField(
        pack,
        structure,
        writer,
        field,
        offsetPrevious
      );
      offsetPrevious = field.offset().add(field.size().value());
    }

    writer.append("} ");
    writer.append(typeName);
    writer.append(";\n");
    writer.append("\n");

    writer.append(
      "static_assert(sizeof(%s) == %s);\n".formatted(
        typeName,
        structure.size()
      )
    );
    writer.append("\n");
  }

  private void executePackageFileTypeStructField(
    final MiPackageType pack,
    final MiStructureType structure,
    final BufferedWriter writer,
    final MiFieldType field,
    final BigInteger offsetPrevious)
    throws IOException, MiExtractorException
  {
    if (!offsetPrevious.equals(field.offset())) {
      this.insertPadding(writer, field.offset().subtract(offsetPrevious));
    }

    switch (field) {
      case final MiBitFieldType bitField -> {
        this.executePackageFileTypeStructFieldBit(
          pack,
          structure,
          writer,
          bitField
        );
      }
      case final MiTypedFieldType typedField -> {
        executePackageFileTypeStructFieldTyped(writer, typedField);
      }
    }
  }

  private void insertPadding(
    final BufferedWriter writer,
    final BigInteger count)
    throws IOException
  {
    writer.append("  uint8_t padding");
    writer.append(this.paddingIndex.toString());
    writer.append("[");
    writer.append(count.toString());
    writer.append("];\n");
    this.paddingIndex = this.paddingIndex.add(BigInteger.ONE);
  }

  private static void executePackageFileTypeStructFieldTyped(
    final BufferedWriter writer,
    final MiTypedFieldType typedField)
    throws IOException
  {
    writer.append("  ");
    writer.append(typeNameOf(typedField.type()));
    writer.append(" ");
    writer.append(typedField.name().value());
    writer.append(";\n");
  }

  private void executePackageFileTypeStructFieldBit(
    final MiPackageType pack,
    final MiStructureType structure,
    final BufferedWriter writer,
    final MiBitFieldType bitField)
    throws IOException, MiExtractorException
  {
    writer.append("  ");

    final var size = bitField.size();
    if (Objects.equals(size, MiSizeOctets.of(1L))) {
      writer.append("uint8_t");
    } else if (Objects.equals(size, MiSizeOctets.of(2L))) {
      writer.append("uint16_t");
    } else if (Objects.equals(size, MiSizeOctets.of(4L))) {
      writer.append("uint32_t");
    } else if (Objects.equals(size, MiSizeOctets.of(8L))) {
      writer.append("uint64_t");
    } else {
      throw this.errorUnsupportedBitFieldType(pack, structure, bitField);
    }

    writer.append(" ");
    writer.append(bitField.name().value());
    writer.append(";\n");
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

  private static String typeNameOf(
    final MiPackageType pack,
    final MiSimpleName name)
  {
    final var packName =
      pack.name()
        .toString()
        .replace('.', '_');

    return String.format("%s_%s", packName, name);
  }

  private static String mapNameOf(
    final MiPackageType pack,
    final MiSimpleName name)
  {
    final var packName =
      pack.name()
        .toString()
        .replace('.', '_');

    return String.format("%s_%s", packName, name);
  }

  private void executePackageFileTypeScalar(
    final MiPackageType pack,
    final BufferedWriter writer,
    final MiScalarType scalar)
    throws IOException, MiExtractorException
  {
    writer.append("typedef ");

    final var size = scalar.size();
    switch (scalar.kind()) {
      case INTEGER_SIGNED -> {
        if (Objects.equals(size, MiSizeOctets.of(1L))) {
          writer.append("int8_t");
        } else if (Objects.equals(size, MiSizeOctets.of(2L))) {
          writer.append("int16_t");
        } else if (Objects.equals(size, MiSizeOctets.of(4L))) {
          writer.append("int32_t");
        } else if (Objects.equals(size, MiSizeOctets.of(8L))) {
          writer.append("int64_t");
        } else {
          throw this.errorUnsupportedScalarType(pack, scalar);
        }
      }
      case INTEGER_UNSIGNED -> {
        if (Objects.equals(size, MiSizeOctets.of(1L))) {
          writer.append("uint8_t");
        } else if (Objects.equals(size, MiSizeOctets.of(2L))) {
          writer.append("uint16_t");
        } else if (Objects.equals(size, MiSizeOctets.of(4L))) {
          writer.append("uint32_t");
        } else if (Objects.equals(size, MiSizeOctets.of(8L))) {
          writer.append("uint64_t");
        } else {
          throw this.errorUnsupportedScalarType(pack, scalar);
        }
      }
      default -> {
        throw this.errorUnsupportedScalarType(pack, scalar);
      }
    }

    writer.append(" ");
    writer.append(typeNameOf(pack, scalar.name()));
    writer.append(";\n");
  }

  private static void executePackageFileMap(
    final MiPackageType pack,
    final BufferedWriter writer,
    final MiMapType map)
    throws IOException
  {
    writer.append(
      String.format(
        "%s * const %s = (%s * const) %s;\n",
        typeNameOf(map.type()),
        mapNameOf(pack, map.name()),
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
}
