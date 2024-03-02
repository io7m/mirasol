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


package com.io7m.mirasol.extractor.pascal.internal;

import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.mirasol.core.MiBitFieldType;
import com.io7m.mirasol.core.MiBitRangeType;
import com.io7m.mirasol.core.MiFieldType;
import com.io7m.mirasol.core.MiMapType;
import com.io7m.mirasol.core.MiPackageName;
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
import static com.io7m.mirasol.strings.MiStringConstants.ERROR_IO;
import static com.io7m.mirasol.strings.MiStringConstants.ERROR_UNSUPPORTED_BIT_FIELD_TYPE;
import static com.io7m.mirasol.strings.MiStringConstants.ERROR_UNSUPPORTED_SCALAR_TYPE;
import static com.io7m.mirasol.strings.MiStringConstants.FIELD;
import static com.io7m.mirasol.strings.MiStringConstants.PACKAGE;
import static com.io7m.mirasol.strings.MiStringConstants.TYPE;

/**
 * An extractor for Pascal.
 */

public final class MiExtractorPascal
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
   * An extractor for Pascal.
   *
   * @param inConfiguration The configuration
   */

  public MiExtractorPascal(
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
    final var pathStart = this.startPath();
    this.writer.append("unit ");
    this.writer.append(pathStart.toCName());
    this.writer.append(";\n");
    this.writer.append('\n');

    this.writer.append("// Automatically generated. DO NOT EDIT.\n");
    this.writer.append("// Extractor: com.io7m.mirasol.extractor.pascal\n");
    this.writer.append("// Package: ");
    this.writer.append(this.packageNow.name().toString());
    this.writer.append('\n');
    this.writer.append('\n');

    this.writer.append("interface\n");
    this.writer.append("\n");
    this.writePackageImports();
    this.writePackageTypes(pathStart);

    if (this.hasMaps() || this.hasBitFields()) {
      this.writer.append("const\n");
    }

    this.writePackageMaps(pathStart);
    this.writeBitFieldConstants();

    this.writer.append("implementation\n");
    this.writer.append("\n");
    this.writer.append("end.\n");
  }

  private void writeBitFieldConstants()
    throws IOException
  {
    if (this.hasBitFields()) {
      for (final var type : this.packageNow.types()) {
        this.writeBitFieldConstantsForType(type);
      }
    }
  }

  private void writeBitFieldConstantsForType(
    final MiTypeType type)
    throws IOException
  {
    switch (type) {
      case final MiScalarType ignored -> {
      }
      case final MiStructureType structure -> {
        this.writeBitFieldConstantsForStructure(
          new MiNamedOffsetPath(List.of(
            new MiNamedOffset(structure.name(), BigInteger.ZERO)
          )),
          structure
        );
      }
    }
  }

  private void writeBitFieldConstantsForStructure(
    final MiNamedOffsetPath path,
    final MiStructureType structure)
    throws IOException
  {
    for (final var field : structure.fields()) {
      switch (field) {
        case final MiBitFieldType bitField -> {
          this.writeStructureBitFieldConstants(
            path.with(new MiNamedOffset(bitField.name(), bitField.offset())),
            bitField);
        }
        case final MiTypedFieldType ignored -> {
        }
      }
    }
  }

  private boolean hasBitFields()
  {
    return this.packageNow.types()
      .stream()
      .anyMatch(t -> switch (t) {
        case final MiScalarType ignored -> false;
        case final MiStructureType structure -> {
          yield structure.fields()
            .stream()
            .anyMatch(f -> f instanceof MiBitFieldType);
        }
      });
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
    if (this.hasMaps()) {
      for (final var map : this.packageNow.maps()) {
        this.writeMap(
          path.with(new MiNamedOffset(map.name(), map.offset())),
          map
        );
      }
      this.writer.append('\n');
    }
  }

  private boolean hasMaps()
  {
    return !this.packageNow.maps().isEmpty();
  }

  private void writePackageTypes(
    final MiNamedOffsetPath path)
    throws IOException, MiExtractorException
  {
    final var typesOrdered = this.packageNow.typesTopological();
    if (!typesOrdered.isEmpty()) {
      this.writer.append("type\n");
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
      this.writer.append("uses ");

      final var unitNames =
        this.packageNow.imports()
        .stream()
        .map(i -> unitNameOf(i.packageName()))
        .collect(Collectors.joining(", "));

      this.writer.append(String.join(", ", unitNames));
      this.writer.append(";\n");
      this.writer.append('\n');
    }
  }

  private static String unitNameOf(
    final MiPackageName name)
  {
    return String.join("_", name.value().segments());
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
    this.writer.append("  ");
    this.writer.append(structure.name().value());
    this.writer.append(" = packed record\n");

    var offsetPrevious = BigInteger.ZERO;

    for (final var field : structure.fields()) {
      this.writeStructureField(
        structure,
        field,
        offsetPrevious
      );
      offsetPrevious = field.offset().add(field.size().value());
    }

    this.writer.append("  end;\n");
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
      "  %s = %s;\n".formatted(
        path.toCName() + "__shift",
        range.range().lower()
      )
    );

    this.writer.append(
      "  %s = %s;\n".formatted(
        path.toCName() + "__mask",
        "%" + mask.toString(2)
      )
    );
  }

  private void writeStructureField(
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
    this.writer.append("    ");
    this.writer.append("padding");
    this.writer.append(this.paddingIndex.toString());
    this.writer.append(" : array [0 .. ");
    this.writer.append(count.subtract(BigInteger.ONE).toString());
    this.writer.append("] of uint8;\n");
    this.paddingIndex = this.paddingIndex.add(BigInteger.ONE);
  }

  private void writeStructureFieldTyped(
    final MiTypedFieldType typedField)
    throws IOException
  {
    this.writer.append("    ");
    this.writer.append(safePascalName(typedField.name()));
    this.writer.append(" : ");
    this.writer.append(this.typeNameOf(typedField.type()));
    this.writer.append(";\n");
  }

  private void writeStructureFieldBit(
    final MiStructureType structure,
    final MiBitFieldType bitField)
    throws IOException, MiExtractorException
  {
    this.writer.append("    ");
    this.writer.append(safePascalName(bitField.name()));
    this.writer.append(" : ");

    final var size = bitField.size();
    if (Objects.equals(size, MiSizeOctets.of(1L))) {
      this.writer.append("uint8");
    } else if (Objects.equals(size, MiSizeOctets.of(2L))) {
      this.writer.append("uint16");
    } else if (Objects.equals(size, MiSizeOctets.of(4L))) {
      this.writer.append("uint32");
    } else if (Objects.equals(size, MiSizeOctets.of(8L))) {
      this.writer.append("uint64");
    } else {
      throw this.errorUnsupportedBitFieldType(structure, bitField);
    }
    this.writer.append(";\n");
  }

  private static String safePascalName(
    final MiSimpleName name)
  {
    return switch (name.value().toUpperCase(Locale.ROOT)) {
      case "IN" -> "_" + name;
      default -> name.value();
    };
  }

  private String typeNameOf(
    final MiTypeReference type)
  {
    if (Objects.equals(type.packageName(), this.packageNow.name())) {
      return type.type().name().value();
    }

    final var packName =
      type.packageName()
        .toString()
        .replace('.', '_');

    return String.format("%s.%s", packName, type.type().name());
  }

  private void writeScalar(
    final MiNamedOffsetPath path,
    final MiScalarType scalar)
    throws IOException, MiExtractorException
  {
    this.writer.append("  ");
    this.writer.append(safePascalName(scalar.name()));
    this.writer.append(" = ");

    final var size = scalar.size();
    switch (scalar.kind()) {
      case INTEGER_SIGNED -> {
        if (Objects.equals(size, MiSizeOctets.of(1L))) {
          this.writer.append("int8");
        } else if (Objects.equals(size, MiSizeOctets.of(2L))) {
          this.writer.append("int16");
        } else if (Objects.equals(size, MiSizeOctets.of(4L))) {
          this.writer.append("int32");
        } else if (Objects.equals(size, MiSizeOctets.of(8L))) {
          this.writer.append("int64");
        } else {
          throw this.errorUnsupportedScalarType(scalar);
        }
      }
      case INTEGER_UNSIGNED -> {
        if (Objects.equals(size, MiSizeOctets.of(1L))) {
          this.writer.append("uint8");
        } else if (Objects.equals(size, MiSizeOctets.of(2L))) {
          this.writer.append("uint16");
        } else if (Objects.equals(size, MiSizeOctets.of(4L))) {
          this.writer.append("uint32");
        } else if (Objects.equals(size, MiSizeOctets.of(8L))) {
          this.writer.append("uint64");
        } else {
          throw this.errorUnsupportedScalarType(scalar);
        }
      }
      default -> {
        throw this.errorUnsupportedScalarType(scalar);
      }
    }

    this.writer.append(";\n");
  }

  private void writeMap(
    final MiNamedOffsetPath path,
    final MiMapType map)
    throws IOException
  {
    this.writer.append("  ");
    this.writer.append(safePascalName(map.name()));
    this.writer.append(" : ^");
    this.writer.append(this.typeNameOf(map.type()));
    this.writer.append(" = pointer($%s);\n".formatted(
      map.offset().toString(16)
    ));
  }

  private static String fileNameOf(
    final MiPackageType pack)
  {
    return "%s.pas".formatted(
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
