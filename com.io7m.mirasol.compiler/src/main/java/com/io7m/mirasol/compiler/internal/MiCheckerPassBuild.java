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
import com.io7m.mirasol.core.MiPackageName;
import com.io7m.mirasol.core.MiScalarKinds;
import com.io7m.mirasol.core.MiScalarType;
import com.io7m.mirasol.core.MiStructureType;
import com.io7m.mirasol.core.MiTypeReference;
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
import java.util.Comparator;

/**
 * A checker pass that builds an output package.
 */

final class MiCheckerPassBuild
  implements MiCheckerPassType
{
  MiCheckerPassBuild()
  {

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
        yield buildScalar(context, scalar);
      }

      case final MiASTStructure structure -> {
        yield this.buildStructure(context, structure);
      }
    };
  }

  private MiMapType buildMap(
    final MiCheckerContext context,
    final MiASTMap map)
    throws MiCheckerException
  {
    final var name = map.name().toSimpleName();
    final var existing = context.buildGet(name);
    if (existing != null) {
      return (MiMapType) existing;
    }

    final var target =
      this.resolve(context, map.type());

    final var result = switch (target.element) {
      case final MiScalarType sc -> {
        yield new MiMap(
          map.name().toSimpleName(),
          new MiTypeReference(
            target.packageName,
            sc
          ),
          map.offset().value(),
          sc.size()
        );
      }
      case final MiStructureType st -> {
        yield new MiMap(
          map.name().toSimpleName(),
          new MiTypeReference(
            target.packageName,
            st
          ),
          map.offset().value(),
          st.size()
        );
      }
      case final MiMapType ignored -> {
        throw new IllegalStateException();
      }
    };

    context.buildSave(result);
    return result;
  }

  private record MiPackageElementReference(
    MiPackageName packageName,
    MiPackageElementType element)
  {

  }

  private MiPackageElementReference resolve(
    final MiCheckerContext context,
    final MiASTTypeReference typeRef)
    throws MiCheckerException
  {
    if (typeRef.prefix().isEmpty()) {
      final var r = context.get(typeRef.name().toSimpleName());
      return new MiPackageElementReference(
        context.source().name().toPackageName(),
        this.buildElement(context, r)
      );
    }

    final var pack =
      context.packageForPrefix(typeRef.prefix().get());

    final var typeReference =
      pack.type(typeRef.name().toSimpleName())
        .orElseThrow();

    return new MiPackageElementReference(
      typeReference.packageName(),
      typeReference.type()
    );
  }

  private MiStructure buildStructure(
    final MiCheckerContext context,
    final MiASTStructure structure)
    throws MiCheckerException
  {
    final var name = structure.name().toSimpleName();
    final var existing = context.buildGet(name);
    if (existing != null) {
      return (MiStructure) existing;
    }

    final var fields = new ArrayList<MiFieldType>();
    for (final var field : structure.fields()) {
      switch (field) {
        case final MiASTBitField bitField -> {
          fields.add(buildBitField(bitField));
        }
        case final MiASTField plainField -> {
          fields.add(this.buildTypedField(context, plainField));
        }
      }
    }

    fields.sort(Comparator.comparing(MiFieldType::offset));

    final var result =
      new MiStructure(name, context.sizeOf(name).orElseThrow(), fields);

    context.buildSave(result);
    return result;
  }

  private MiFieldType buildTypedField(
    final MiCheckerContext context,
    final MiASTField plainField)
    throws MiCheckerException
  {
    final var elementReference =
      this.resolve(context, plainField.type());

    return new MiTypedField(
      plainField.name().toSimpleName(),
      plainField.offset().value(),
      new MiTypeReference(
        elementReference.packageName,
        (MiTypeType) elementReference.element
      )
    );
  }

  private static MiFieldType buildBitField(
    final MiASTBitField bitField)
  {
    final var ranges = bitField.ranges();
    final var output = new ArrayList<MiBitRangeType>();
    for (final var r : ranges) {
      output.add(new MiBitRange(r.name().toSimpleName(), r.range()));
    }
    output.sort(Comparator.comparing(MiBitRangeType::range));

    return new MiBitField(
      bitField.name().toSimpleName(),
      bitField.offset().value(),
      bitField.sizeOctets(),
      output
    );
  }

  private static MiScalar buildScalar(
    final MiCheckerContext context,
    final MiASTScalarTypeDeclaration scalar)
  {
    final var name = scalar.name().toSimpleName();
    final var existing = context.buildGet(name);
    if (existing != null) {
      return (MiScalar) existing;
    }

    final var scalarType =
      new MiScalar(
        name,
        MiScalarKinds.of(scalar.kind().value()),
        context.sizeOf(name).orElseThrow(),
        scalar.size()
      );

    context.buildSave(scalarType);
    return scalarType;
  }
}
