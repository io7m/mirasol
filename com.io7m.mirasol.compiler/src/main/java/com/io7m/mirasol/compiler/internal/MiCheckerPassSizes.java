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

import com.io7m.abstand.core.IntervalB;
import com.io7m.abstand.core.IntervalTree;
import com.io7m.abstand.core.IntervalTreeDebuggableType;
import com.io7m.abstand.core.IntervalType;
import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.mirasol.parser.api.ast.MiASTBitField;
import com.io7m.mirasol.parser.api.ast.MiASTBitRange;
import com.io7m.mirasol.parser.api.ast.MiASTField;
import com.io7m.mirasol.parser.api.ast.MiASTFieldType;
import com.io7m.mirasol.parser.api.ast.MiASTImportDeclaration;
import com.io7m.mirasol.parser.api.ast.MiASTMap;
import com.io7m.mirasol.parser.api.ast.MiASTPackageElementType;
import com.io7m.mirasol.parser.api.ast.MiASTScalarTypeDeclaration;
import com.io7m.mirasol.parser.api.ast.MiASTSizeAssertion;
import com.io7m.mirasol.parser.api.ast.MiASTStructure;
import com.io7m.mirasol.parser.api.ast.MiASTTypeReference;
import com.io7m.seltzer.api.SStructuredError;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import static com.io7m.mirasol.strings.MiStringConstants.BIT_FIELD;
import static com.io7m.mirasol.strings.MiStringConstants.BIT_FIELD_CONFLICTING;
import static com.io7m.mirasol.strings.MiStringConstants.BIT_FIELD_CURRENT;
import static com.io7m.mirasol.strings.MiStringConstants.ERROR_CHECKER_BIT_FIELD_OVERLAP;
import static com.io7m.mirasol.strings.MiStringConstants.ERROR_CHECKER_BIT_FIELD_SIZE_INSUFFICIENT;
import static com.io7m.mirasol.strings.MiStringConstants.ERROR_CHECKER_FIELD_OVERLAP;
import static com.io7m.mirasol.strings.MiStringConstants.ERROR_CHECKER_SIZE_POSITIVE;
import static com.io7m.mirasol.strings.MiStringConstants.FIELD_CONFLICTING;
import static com.io7m.mirasol.strings.MiStringConstants.FIELD_CURRENT;
import static com.io7m.mirasol.strings.MiStringConstants.PACKAGE;
import static com.io7m.mirasol.strings.MiStringConstants.SIZE_BITS;
import static com.io7m.mirasol.strings.MiStringConstants.SIZE_OCTETS_ACTUAL;
import static com.io7m.mirasol.strings.MiStringConstants.SIZE_OCTETS_REQUIRED;
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

    return type.type().size();
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

    final var tracker =
      new ExceptionTracker<MiCheckerException>();
    final var intervals =
      IntervalTree.<BigInteger>create();
    final var intervalsByField =
      new HashMap<IntervalB, MiASTFieldType>();

    var sizeOctets = BigInteger.ZERO;

    for (final var field : structure.fields()) {
      switch (field) {
        case final MiASTBitField bitField -> {
          validateBitField(context, structure, bitField);

          final var fieldSize =
            bitField.sizeOctets();
          final var fieldOffset =
            bitField.offset().value();

          try {
            validateFieldOverlap(
              context,
              structure,
              bitField,
              fieldOffset,
              fieldSize,
              intervalsByField,
              intervals);
          } catch (final MiCheckerException e) {
            tracker.addException(e);
          }

          sizeOctets = sizeOctets.max(fieldOffset.add(fieldSize));
        }

        case final MiASTField plainField -> {
          final var fieldSize =
            this.sizeOf(context, plainField.type());
          final var fieldOffset =
            plainField.offset().value();

          try {
            validateFieldOverlap(
              context,
              structure,
              plainField,
              fieldOffset,
              fieldSize,
              intervalsByField,
              intervals
            );
          } catch (final MiCheckerException e) {
            tracker.addException(e);
          }

          sizeOctets = sizeOctets.max(fieldOffset.add(fieldSize));
        }
      }
    }

    try {
      this.validateStructureSizeAssertion(context, structure, sizeOctets);
    } catch (final MiCheckerException e) {
      tracker.addException(e);
    }

    context.sizeSave(name, sizeOctets);
    tracker.throwIfNecessary();
    return sizeOctets;
  }

  private static void validateStructureSizeAssertion(
    final MiCheckerContext context,
    final MiASTStructure structure,
    final BigInteger sizeOctets)
    throws MiCheckerException
  {
    final var sizeAssertionOpt = structure.sizeAssertion();
    if (sizeAssertionOpt.isPresent()) {
      final var sizeAssertion = sizeAssertionOpt.get();
      if (!Objects.equals(sizeAssertion.value(), sizeOctets)) {
        throw errorSizeAssertionFailed(context, structure, sizeAssertion, sizeOctets);
      }
    }
  }

  private static void validateFieldOverlap(
    final MiCheckerContext context,
    final MiASTStructure structure,
    final MiASTFieldType bitField,
    final BigInteger fieldOffset,
    final BigInteger fieldSize,
    final HashMap<IntervalB, MiASTFieldType> intervalsByField,
    final IntervalTreeDebuggableType<BigInteger> intervals)
    throws MiCheckerException
  {
    final var fieldInterval =
      new IntervalB(fieldOffset, fieldOffset.add(subtractOne(fieldSize)));

    final var conflicting = intervalsByField.get(fieldInterval);
    if (conflicting != null) {
      throw errorFieldOverlap(context, structure, bitField, conflicting);
    }

    intervals.add(fieldInterval);
    intervalsByField.put(fieldInterval, bitField);

    final var overlaps =
      intervals.overlapping(fieldInterval)
        .stream()
        .filter(r -> !Objects.equals(r, fieldInterval))
        .toList();

    if (!overlaps.isEmpty()) {
      final var conflictingOver = intervalsByField.get(overlaps.get(0));
      throw errorFieldOverlap(context, structure, bitField, conflictingOver);
    }
  }

  private static BigInteger subtractOne(
    final BigInteger x)
  {
    return BigInteger.ZERO.max(x.subtract(BigInteger.ONE));
  }

  /**
   * Validate that all constraints hold for a given bit field.
   */

  private static void validateBitField(
    final MiCheckerContext context,
    final MiASTStructure structure,
    final MiASTBitField bitField)
    throws MiCheckerException
  {
    final var tracker =
      new ExceptionTracker<MiCheckerException>();
    final var intervals =
      IntervalTree.<BigInteger>create();
    final var intervalsByField =
      new HashMap<IntervalB, MiASTBitRange>();

    validateBitFieldRangesNoOverlap(
      context,
      tracker, structure,
      bitField,
      intervalsByField,
      intervals
    );

    try {
      validateBitFieldSizeSufficient(
        context, structure, bitField, intervals.maximum()
      );
    } catch (final MiCheckerException e) {
      tracker.addException(e);
    }

    tracker.throwIfNecessary();
  }

  /**
   * Validate that there are no overlapping bit ranges in the given bit field.
   */

  private static void validateBitFieldRangesNoOverlap(
    final MiCheckerContext context,
    final ExceptionTracker<MiCheckerException> tracker,
    final MiASTStructure structure,
    final MiASTBitField bitField,
    final HashMap<IntervalB, MiASTBitRange> intervalsByField,
    final IntervalTreeDebuggableType<BigInteger> intervals)
  {
    for (final var current : bitField.ranges()) {
      final var interval = current.range();
      final var conflicting = intervalsByField.get(interval);
      if (conflicting != null) {
        tracker.addException(
          errorBitRangeOverlap(context, structure, current, conflicting)
        );
      }

      intervalsByField.put(interval, current);
      intervals.add(interval);

      final var overlaps =
        intervals.overlapping(interval)
          .stream()
          .filter(r -> !Objects.equals(r, current.range()))
          .toList();

      if (!overlaps.isEmpty()) {
        final var conflictingOver = intervalsByField.get(overlaps.get(0));
        tracker.addException(
          errorBitRangeOverlap(context, structure, current, conflictingOver)
        );
      }
    }
  }

  /**
   * Validate that specified size of a bit field is large enough to contain
   * all the bit ranges.
   */

  private static void validateBitFieldSizeSufficient(
    final MiCheckerContext context,
    final MiASTStructure structure,
    final MiASTBitField bitField,
    final Optional<IntervalType<BigInteger>> maximumOpt)
    throws MiCheckerException
  {
    if (maximumOpt.isPresent()) {
      final var maximum =
        maximumOpt.get();
      final var q =
        maximum.upper().divideAndRemainder(BigInteger.valueOf(8L));

      final var octets = q[0];
      final var remainder = q[1];

      final BigInteger octetsRequired;
      if (remainder.compareTo(BigInteger.ZERO) > 0) {
        octetsRequired = octets.add(BigInteger.ONE);
      } else {
        octetsRequired = octets;
      }

      if (octetsRequired.compareTo(bitField.sizeOctets()) > 0) {
        throw errorBitFieldSizeInsufficient(
          context,
          structure,
          bitField,
          octetsRequired,
          bitField.sizeOctets()
        );
      }
    }
  }

  private static MiCheckerException errorSizeAssertionFailed(
    final MiCheckerContext context,
    final MiASTStructure structure,
    final MiASTSizeAssertion sizeAssertion,
    final BigInteger sizeOctets)
  {
    final var attributes = new TreeMap<String, String>();
    context.putLexicalPosition(attributes, sizeAssertion.lexical());

    attributes.put(
      context.format(PACKAGE),
      context.source().name().toPackageName().toString()
    );
    attributes.put(
      context.format(TYPE),
      structure.name().value()
    );
    attributes.put(
      context.format(SIZE_OCTETS_REQUIRED),
      sizeAssertion.value().toString()
    );
    attributes.put(
      context.format(SIZE_OCTETS_ACTUAL),
      sizeOctets.toString()
    );

    return context.error(
      new SStructuredError<>(
        "error-size-assertion-failed",
        context.format(ERROR_CHECKER_BIT_FIELD_SIZE_INSUFFICIENT),
        attributes,
        Optional.empty(),
        Optional.empty()
      )
    );
  }


  private static MiCheckerException errorBitFieldSizeInsufficient(
    final MiCheckerContext context,
    final MiASTStructure structure,
    final MiASTBitField bitField,
    final BigInteger required,
    final BigInteger provided)
  {
    final var attributes = new TreeMap<String, String>();
    context.putLexicalPosition(attributes, bitField.lexical());

    attributes.put(
      context.format(PACKAGE),
      context.source().name().toPackageName().toString()
    );
    attributes.put(
      context.format(TYPE),
      structure.name().value()
    );
    attributes.put(
      context.format(BIT_FIELD),
      bitField.name().value()
    );
    attributes.put(
      context.format(SIZE_OCTETS_REQUIRED),
      required.toString()
    );
    attributes.put(
      context.format(SIZE_OCTETS_ACTUAL),
      provided.toString()
    );

    return context.error(
      new SStructuredError<>(
        "error-bit-field-size-insufficient",
        context.format(ERROR_CHECKER_BIT_FIELD_SIZE_INSUFFICIENT),
        attributes,
        Optional.empty(),
        Optional.empty()
      )
    );
  }

  private static MiCheckerException errorBitRangeOverlap(
    final MiCheckerContext context,
    final MiASTStructure structure,
    final MiASTBitRange current,
    final MiASTBitRange conflicting)
  {
    final var attributes = new TreeMap<String, String>();
    context.putLexicalPosition(attributes, current.lexical());

    attributes.put(
      context.format(PACKAGE),
      context.source().name().toPackageName().toString()
    );
    attributes.put(
      context.format(TYPE),
      structure.name().value()
    );
    attributes.put(
      context.format(BIT_FIELD_CURRENT),
      current.name().value()
    );
    attributes.put(
      context.format(BIT_FIELD_CONFLICTING),
      conflicting.name().value()
    );

    return context.error(
      new SStructuredError<>(
        "error-bit-field-overlap",
        context.format(ERROR_CHECKER_BIT_FIELD_OVERLAP),
        attributes,
        Optional.empty(),
        Optional.empty()
      )
    );
  }

  private static MiCheckerException errorFieldOverlap(
    final MiCheckerContext context,
    final MiASTStructure structure,
    final MiASTFieldType current,
    final MiASTFieldType conflicting)
  {
    final var attributes = new TreeMap<String, String>();
    context.putLexicalPosition(attributes, current.lexical());

    attributes.put(
      context.format(PACKAGE),
      context.source().name().toPackageName().toString()
    );
    attributes.put(
      context.format(TYPE),
      structure.name().value()
    );
    attributes.put(
      context.format(FIELD_CURRENT),
      current.name().value()
    );
    attributes.put(
      context.format(FIELD_CONFLICTING),
      conflicting.name().value()
    );

    return context.error(
      new SStructuredError<>(
        "error-field-overlap",
        context.format(ERROR_CHECKER_FIELD_OVERLAP),
        attributes,
        Optional.empty(),
        Optional.empty()
      )
    );
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
      context.format(PACKAGE),
      context.source().name().toPackageName().toString()
    );
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
