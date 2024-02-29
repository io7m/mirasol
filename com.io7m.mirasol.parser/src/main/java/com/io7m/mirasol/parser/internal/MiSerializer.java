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


package com.io7m.mirasol.parser.internal;

import com.io7m.anethum.api.SerializationException;
import com.io7m.mirasol.core.MiBitFieldType;
import com.io7m.mirasol.core.MiBitRangeType;
import com.io7m.mirasol.core.MiFieldType;
import com.io7m.mirasol.core.MiMapType;
import com.io7m.mirasol.core.MiPackageElementType;
import com.io7m.mirasol.core.MiPackageReference;
import com.io7m.mirasol.core.MiPackageType;
import com.io7m.mirasol.core.MiScalarType;
import com.io7m.mirasol.core.MiStructureType;
import com.io7m.mirasol.core.MiTypeReference;
import com.io7m.mirasol.core.MiTypedFieldType;
import com.io7m.mirasol.parser.api.MiSerializerOptions;
import com.io7m.mirasol.parser.api.MiSerializerType;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * The default package serializer.
 */

public final class MiSerializer
  implements MiSerializerType
{
  private static final String NS = "urn:com.io7m.mirasol:1";

  private final URI target;
  private final OutputStream stream;
  private final XMLStreamWriter output;
  private final ByteArrayOutputStream byteStream;
  private final MiSerializerOptions options;

  /**
   * The default package serializer.
   *
   * @param inStream  The output stream
   * @param inTarget  The target URI
   * @param inOptions The serializer options
   */

  public MiSerializer(
    final MiSerializerOptions inOptions,
    final URI inTarget,
    final OutputStream inStream)
  {
    this.options =
      Objects.requireNonNull(inOptions, "inOptions");
    this.target =
      Objects.requireNonNull(inTarget, "target");
    this.stream =
      Objects.requireNonNull(inStream, "stream");
    this.byteStream =
      new ByteArrayOutputStream();

    try {
      this.output =
        XMLOutputFactory.newFactory()
          .createXMLStreamWriter(this.byteStream, "UTF-8");
    } catch (final XMLStreamException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void execute(
    final MiPackageType value)
    throws SerializationException
  {
    try {
      this.output.writeStartDocument("UTF-8", "1.0");
      this.serializePackage(value);
      this.output.writeEndDocument();
      this.indent();
    } catch (final XMLStreamException | TransformerException | IOException e) {
      throw new SerializationException(e.getMessage(), e);
    }
  }

  private void indent()
    throws TransformerException, IOException
  {
    final var source =
      new StreamSource(new ByteArrayInputStream(this.byteStream.toByteArray()));
    final var result =
      new StreamResult(this.stream);

    this.stream.write(
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n"
        .getBytes(StandardCharsets.UTF_8)
    );

    final var transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(
      "{http://xml.apache.org/xslt}indent-amount",
      "2");
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    transformer.transform(source, result);
  }

  private void serializePackage(
    final MiPackageType pack)
    throws XMLStreamException
  {
    this.output.writeStartElement("Package");
    this.output.writeDefaultNamespace(NS);
    this.output.writeAttribute("Name", pack.name().toString());

    this.output.writeStartElement("Documentation");
    this.output.writeCharacters(pack.documentation());
    this.output.writeEndElement();

    for (final var importE : pack.imports()) {
      this.serializePackageImport(importE);
    }

    for (final var element : pack) {
      this.serializePackageElement(pack, element);
    }

    this.output.writeEndElement();
  }

  private void serializePackageImport(
    final MiPackageReference importE)
    throws XMLStreamException
  {
    this.output.writeStartElement("Import");
    this.output.writeAttribute("Package", importE.packageName().toString());
    this.output.writeAttribute("As", importE.alias().toString());
    this.output.writeEndElement();
  }

  private void serializePackageElement(
    final MiPackageType pack,
    final MiPackageElementType element)
    throws XMLStreamException
  {
    switch (element) {
      case final MiMapType map -> {
        this.serializePackageElementMap(pack, map);
      }
      case final MiScalarType scalar -> {
        this.serializePackageElementScalar(scalar);
      }
      case final MiStructureType structure -> {
        this.serializePackageElementStructure(pack, structure);
      }
    }
  }

  private void serializePackageElementStructure(
    final MiPackageType pack,
    final MiStructureType structure)
    throws XMLStreamException
  {
    this.output.writeStartElement("Structure");
    this.output.writeAttribute("Name", structure.name().value());

    for (final var field : structure.fields()) {
      this.serializePackageElementStructureField(pack, field);
    }

    this.output.writeEndElement();
  }

  private void serializePackageElementStructureField(
    final MiPackageType pack,
    final MiFieldType field)
    throws XMLStreamException
  {
    switch (field) {
      case final MiBitFieldType bitField -> {
        this.serializePackageElementStructureBitField(pack, bitField);
      }
      case final MiTypedFieldType typedField -> {
        this.serializePackageElementStructureTypedField(pack, typedField);
      }
    }
  }

  private void serializePackageElementStructureTypedField(
    final MiPackageType pack,
    final MiTypedFieldType typedField)
    throws XMLStreamException
  {
    this.output.writeStartElement("Field");
    this.output.writeAttribute("Name", typedField.name().value());
    this.serializeOffset(typedField.offset());
    this.serializeTypeReference(pack, typedField.type());
    this.output.writeEndElement();
  }

  private void serializeTypeReference(
    final MiPackageType pack,
    final MiTypeReference type)
    throws XMLStreamException
  {
    this.output.writeStartElement("Type");

    if (!Objects.equals(type.packageName(), pack.name())) {
      this.output.writeAttribute(
        "Package",
        pack.imports()
          .stream()
          .filter(i -> Objects.equals(i.packageName(), type.packageName()))
          .findFirst()
          .orElseThrow()
          .alias()
          .toString()
      );
    }

    this.output.writeAttribute("Name", type.type().name().toString());
    this.output.writeEndElement();
  }

  private void serializeOffset(
    final BigInteger offset)
    throws XMLStreamException
  {
    if (this.options.hexOffsets()) {
      this.output.writeStartElement("OffsetHex");
      this.output.writeAttribute("Value", "0x" + offset.toString(16));
      this.output.writeEndElement();
    } else {
      this.output.writeStartElement("Offset");
      this.output.writeAttribute("Value", offset.toString());
      this.output.writeEndElement();
    }
  }

  private void serializePackageElementStructureBitField(
    final MiPackageType pack,
    final MiBitFieldType bitField)
    throws XMLStreamException
  {
    this.output.writeStartElement("BitField");
    this.output.writeAttribute("Name", bitField.name().value());
    this.serializeOffset(bitField.offset());

    for (final var bitRange : bitField.ranges()) {
      this.serializePackageElementStructureBitRange(pack, bitField, bitRange);
    }

    this.output.writeEndElement();
  }

  private void serializePackageElementStructureBitRange(
    final MiPackageType pack,
    final MiBitFieldType bitField,
    final MiBitRangeType bitRange)
    throws XMLStreamException
  {
    this.output.writeStartElement("BitRange");
    this.output.writeAttribute("Name", bitRange.name().value());
    final var range = bitRange.range();
    this.output.writeAttribute("LowerInclusive", range.lower().toString());
    this.output.writeAttribute("UpperInclusive", range.upper().toString());
    this.output.writeEndElement();
  }

  private void serializePackageElementScalar(
    final MiScalarType scalar)
    throws XMLStreamException
  {
    this.output.writeStartElement("ScalarType");
    this.output.writeAttribute("Name", scalar.name().value());
    this.output.writeAttribute("Kind", scalar.kind().value());
    this.output.writeAttribute("SizeInBits", scalar.size().toString());
    this.output.writeEndElement();
  }

  private void serializePackageElementMap(
    final MiPackageType pack,
    final MiMapType map)
    throws XMLStreamException
  {
    this.output.writeStartElement("Map");
    this.output.writeAttribute("Name", map.name().value());
    this.serializeOffset(map.offset());
    this.serializeTypeReference(pack, map.type());
    this.output.writeEndElement();
  }

  @Override
  public void close()
    throws IOException
  {
    this.stream.close();
  }
}
