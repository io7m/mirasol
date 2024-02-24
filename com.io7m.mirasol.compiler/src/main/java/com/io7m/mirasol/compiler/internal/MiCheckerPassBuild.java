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

import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.mirasol.core.MiBitRangeType;
import com.io7m.mirasol.core.MiFieldType;
import com.io7m.mirasol.core.MiMapType;
import com.io7m.mirasol.core.MiPackageElementType;
import com.io7m.mirasol.core.MiScalarType;
import com.io7m.mirasol.core.MiSimpleName;
import com.io7m.mirasol.core.MiStructureType;
import com.io7m.mirasol.core.MiTypeType;
import com.io7m.mirasol.parser.api.ast.MiASTBitField;
import com.io7m.mirasol.parser.api.ast.MiASTField;
import com.io7m.mirasol.parser.api.ast.MiASTImportDeclaration;
import com.io7m.mirasol.parser.api.ast.MiASTMap;
import com.io7m.mirasol.parser.api.ast.MiASTPackageElementType;
import com.io7m.mirasol.parser.api.ast.MiASTScalarTypeDeclaration;
import com.io7m.mirasol.parser.api.ast.MiASTStructure;
import com.io7m.mirasol.parser.api.ast.MiASTTypeReference;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A checker pass that builds an output package.
 */

final class MiCheckerPassBuild
  implements MiCheckerPassType
{
  private final HashMap<MiSimpleName, MiPackageElementType> elements;

  MiCheckerPassBuild()
  {
    this.elements = new HashMap<>();
  }

  @Override
  public void execute(
    final MiCheckerContext context)
    throws MiCheckerException
  {
    final var tracker = new ExceptionTracker<MiCheckerException>();

    for (final var element : context.source().elements()) {
      try {
        this.buildElement(context, element);
      } catch (final MiCheckerException e) {
        tracker.addException(e);
      }
    }

    tracker.throwIfNecessary();
  }

  private MiPackageElementType buildElement(
    final MiCheckerContext context,
    final MiASTPackageElementType element)
    throws MiCheckerException
  {
    return switch (element) {
      case final MiASTImportDeclaration ignored -> {
        yield null;
      }

      case final MiASTMap map -> {
        yield this.buildMap(context, map);
      }

      case final MiASTScalarTypeDeclaration scalar -> {
        yield this.buildScalar(context, scalar);
      }

      case final MiASTStructure structure -> {
        yield this.buildStructure(context, structure);
      }
    };
  }

  private MiPackageElementType buildMap(
    final MiCheckerContext context,
    final MiASTMap map)
    throws MiCheckerException
  {
    final var name = map.name().toSimpleName();
    final var existing = this.elements.get(name);
    if (existing != null) {
      return existing;
    }

    final var target =
      this.resolve(context, map.type());

    return switch (target) {
      case final MiScalarType sc -> {
        yield new MiMap(
          map.name().toSimpleName(),
          sc,
          map.offset().value(),
          sc.size()
        );
      }
      case final MiStructureType st -> {
        yield new MiMap(
          map.name().toSimpleName(),
          st,
          map.offset().value(),
          st.size()
        );
      }
      case final MiMapType ignored -> {
        throw new IllegalStateException();
      }
    };
  }

  private MiPackageElementType resolve(
    final MiCheckerContext context,
    final MiASTTypeReference typeRef)
    throws MiCheckerException
  {
    if (typeRef.prefix().isEmpty()) {
      final var r = context.get(typeRef.name().toSimpleName());
      return this.buildElement(context, r);
    }

    final var pack =
      context.packageForPrefix(typeRef.prefix().get());

    return pack.type(typeRef.name().toSimpleName())
      .orElseThrow();
  }

  private MiPackageElementType buildStructure(
    final MiCheckerContext context,
    final MiASTStructure structure)
    throws MiCheckerException
  {
    final var name = structure.name().toSimpleName();
    final var existing = this.elements.get(name);
    if (existing != null) {
      return existing;
    }

    final var fields = new ArrayList<MiFieldType>();
    for (final var field : structure.fields()) {
      switch (field) {
        case final MiASTBitField bitField -> {
          fields.add(this.buildBitField(context, bitField));
        }
        case final MiASTField plainField -> {
          fields.add(this.buildTypedField(context, plainField));
        }
      }
    }

    return new MiStructure(
      name,
      context.sizeOf(name).orElseThrow(),
      fields
    );
  }

  private MiFieldType buildTypedField(
    final MiCheckerContext context,
    final MiASTField plainField)
    throws MiCheckerException
  {
    return new MiTypedField(
      plainField.name().toSimpleName(),
      plainField.offset().value(),
      (MiTypeType) this.resolve(context, plainField.type())
    );
  }

  private MiFieldType buildBitField(
    final MiCheckerContext context,
    final MiASTBitField bitField)
  {
    final var ranges = bitField.ranges();
    final var output = new ArrayList<MiBitRangeType>();
    for (final var r : ranges) {
      output.add(
        new MiBitRange(
          r.name().toSimpleName(),
          r.range().lower(),
          r.range().upper()
        )
      );
    }

    return new MiBitField(
      bitField.name().toSimpleName(),
      bitField.offset().value(),
      bitField.sizeOctets(),
      output
    );
  }

  private MiPackageElementType buildScalar(
    final MiCheckerContext context,
    final MiASTScalarTypeDeclaration scalar)
  {
    final var name = scalar.name().toSimpleName();
    final var existing = this.elements.get(name);
    if (existing != null) {
      return existing;
    }

    final var scalarType =
      new MiScalar(
        name,
        scalar.kind().toSimpleName(),
        context.sizeOf(name).orElseThrow()
      );

    context.buildSave(scalarType);
    return scalarType;
  }
}
