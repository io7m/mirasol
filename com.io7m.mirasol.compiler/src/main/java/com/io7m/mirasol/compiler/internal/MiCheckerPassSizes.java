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
import com.io7m.mirasol.parser.api.ast.MiASTBitField;
import com.io7m.mirasol.parser.api.ast.MiASTField;
import com.io7m.mirasol.parser.api.ast.MiASTImportDeclaration;
import com.io7m.mirasol.parser.api.ast.MiASTMap;
import com.io7m.mirasol.parser.api.ast.MiASTPackageElementType;
import com.io7m.mirasol.parser.api.ast.MiASTScalarTypeDeclaration;
import com.io7m.mirasol.parser.api.ast.MiASTStructure;
import com.io7m.mirasol.parser.api.ast.MiASTTypeReference;
import com.io7m.seltzer.api.SStructuredError;

import java.math.BigInteger;
import java.util.Optional;
import java.util.TreeMap;

import static com.io7m.mirasol.strings.MiStringConstants.ERROR_CHECKER_SIZE_POSITIVE;
import static com.io7m.mirasol.strings.MiStringConstants.SIZE_BITS;
import static com.io7m.mirasol.strings.MiStringConstants.TYPE;

/**
 * A checker pass that calculates the sizes of types.
 */

final class MiCheckerPassSizes
  implements MiCheckerPassType
{
  MiCheckerPassSizes()
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
        this.sizeOfElement(context, element);
      } catch (final MiCheckerException e) {
        tracker.addException(e);
      }
    }

    tracker.throwIfNecessary();
  }

  private BigInteger sizeOfElement(
    final MiCheckerContext context,
    final MiASTPackageElementType element)
    throws MiCheckerException
  {
    return switch (element) {
      case final MiASTImportDeclaration ignored -> {
        yield BigInteger.ZERO;
      }

      case final MiASTMap map -> {
        yield this.sizeMap(context, map);
      }

      case final MiASTScalarTypeDeclaration scalar -> {
        yield sizeOfScalar(context, scalar);
      }

      case final MiASTStructure structure -> {
        yield this.sizeOfStructure(context, structure);
      }
    };
  }

  private BigInteger sizeMap(
    final MiCheckerContext context,
    final MiASTMap map)
    throws MiCheckerException
  {
    final var name = map.name().toSimpleName();
    final var existing = context.sizeOf(name);
    if (existing.isPresent()) {
      return existing.get();
    }

    final var sizeOctets = this.sizeOf(context, map.type());
    context.sizeSave(name, sizeOctets);
    return sizeOctets;
  }

  private BigInteger sizeOf(
    final MiCheckerContext context,
    final MiASTTypeReference typeRef)
    throws MiCheckerException
  {
    if (typeRef.prefix().isEmpty()) {
      final var r = context.get(typeRef.name().toSimpleName());
      return this.sizeOfElement(context, r);
    }

    final var pack =
      context.packageForPrefix(typeRef.prefix().get());
    final var type =
      pack.type(typeRef.name().toSimpleName())
        .orElseThrow();

    return type.size();
  }

  private BigInteger sizeOfStructure(
    final MiCheckerContext context,
    final MiASTStructure structure)
    throws MiCheckerException
  {
    final var name = structure.name().toSimpleName();
    final var existing = context.sizeOf(name);
    if (existing.isPresent()) {
      return existing.get();
    }

    var sizeOctets = BigInteger.ZERO;

    for (final var field : structure.fields()) {
      switch (field) {
        case final MiASTBitField bitField -> {
          sizeOctets = sizeOctets.max(
            bitField.offset().value()
              .add(bitField.sizeOctets())
          );
        }
        case final MiASTField plainField -> {
          sizeOctets = sizeOctets.max(
            plainField.offset().value()
              .add(this.sizeOf(context, plainField.type()))
          );
        }
      }
    }

    context.sizeSave(name, sizeOctets);
    return sizeOctets;
  }

  private static BigInteger sizeOfScalar(
    final MiCheckerContext context,
    final MiASTScalarTypeDeclaration scalar)
    throws MiCheckerException
  {
    final var name = scalar.name().toSimpleName();
    final var existing = context.sizeOf(name);
    if (existing.isPresent()) {
      return existing.get();
    }

    final var sizeBits = scalar.size();
    if (sizeBits.compareTo(BigInteger.ZERO) <= 0) {
      throw errorSizeMustBePositive(context, scalar);
    }

    final var sizeOctetsR =
      sizeBits.divideAndRemainder(BigInteger.valueOf(8L));

    var sizeOctets = sizeOctetsR[0];
    if (sizeOctetsR[1].compareTo(BigInteger.ZERO) > 0) {
      sizeOctets = sizeOctets.add(BigInteger.ONE);
    }

    context.sizeSave(name, sizeOctets);
    return sizeOctets;
  }

  private static MiCheckerException errorSizeMustBePositive(
    final MiCheckerContext context,
    final MiASTScalarTypeDeclaration scalar)
  {
    final var attributes = new TreeMap<String, String>();
    context.putLexicalPosition(attributes, scalar.lexical());

    attributes.put(
      context.format(TYPE),
      scalar.name().value()
    );
    attributes.put(
      context.format(SIZE_BITS),
      scalar.size().toString()
    );

    return context.error(
      new SStructuredError<>(
        "error-type-size-positive",
        context.format(ERROR_CHECKER_SIZE_POSITIVE),
        attributes,
        Optional.empty(),
        Optional.empty()
      )
    );
  }
}
