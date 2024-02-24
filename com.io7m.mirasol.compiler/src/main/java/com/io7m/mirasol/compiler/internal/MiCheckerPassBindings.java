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
import com.io7m.mirasol.core.MiSimpleName;
import com.io7m.mirasol.parser.api.ast.MiASTBitField;
import com.io7m.mirasol.parser.api.ast.MiASTField;
import com.io7m.mirasol.parser.api.ast.MiASTFieldType;
import com.io7m.mirasol.parser.api.ast.MiASTImportDeclaration;
import com.io7m.mirasol.parser.api.ast.MiASTMap;
import com.io7m.mirasol.parser.api.ast.MiASTScalarTypeDeclaration;
import com.io7m.mirasol.parser.api.ast.MiASTStructure;
import com.io7m.mirasol.parser.api.ast.MiASTTypeReference;
import com.io7m.seltzer.api.SStructuredError;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.Optional;
import java.util.TreeMap;

import static com.io7m.mirasol.strings.MiStringConstants.ERROR_CHECKER_TYPE_REFERENCE_CYCLIC;
import static com.io7m.mirasol.strings.MiStringConstants.ERROR_CHECKER_TYPE_REFERENCE_MAP;
import static com.io7m.mirasol.strings.MiStringConstants.ERROR_CHECKER_TYPE_REFERENCE_NONEXISTENT;
import static com.io7m.mirasol.strings.MiStringConstants.MAP;
import static com.io7m.mirasol.strings.MiStringConstants.PREFIX;
import static com.io7m.mirasol.strings.MiStringConstants.TYPE;

/**
 * A checker pass that verifies the referential integrity of names.
 */

final class MiCheckerPassBindings
  implements MiCheckerPassType
{
  private DirectedAcyclicGraph<TypePeer, TypePeerReference> graph;

  MiCheckerPassBindings()
  {
    this.graph = new DirectedAcyclicGraph<>(TypePeerReference.class);
  }

  @Override
  public void execute(
    final MiCheckerContext context)
    throws MiCheckerException
  {
    this.checkBindingsCollectInitial(context);
    this.checkBindingsActual(context);
  }

  private void checkBindingsCollectInitial(
    final MiCheckerContext context)
  {
    for (final var element : context.source().elements()) {
      switch (element) {
        case final MiASTImportDeclaration ignored -> {
          // Nothing
        }

        case final MiASTMap map -> {
          context.bind(map);
        }

        case final MiASTScalarTypeDeclaration typeDeclaration -> {
          context.bind(typeDeclaration);
        }

        case final MiASTStructure structure -> {
          context.bind(structure);
          this.graph.addVertex(new TypePeer(structure.name().toSimpleName()));
        }
      }
    }
  }

  private void checkBindingsActual(
    final MiCheckerContext context)
    throws MiCheckerException
  {
    final var tracker = new ExceptionTracker<MiCheckerException>();

    for (final var element : context.source().elements()) {
      switch (element) {
        case final MiASTImportDeclaration ignored -> {
          // Nothing
        }
        case final MiASTMap map -> {
          try {
            checkBindingsMap(context, map);
          } catch (final MiCheckerException e) {
            tracker.addException(e);
          }
        }
        case final MiASTScalarTypeDeclaration typeDeclaration -> {
          this.checkBindingsTypeDeclaration(typeDeclaration);
        }
        case final MiASTStructure structure -> {
          this.checkBindingsStructure(context, structure);
        }
      }
    }

    tracker.throwIfNecessary();
  }

  private void checkBindingsStructure(
    final MiCheckerContext context,
    final MiASTStructure structure)
    throws MiCheckerException
  {
    final var tracker = new ExceptionTracker<MiCheckerException>();

    for (final var field : structure.fields()) {
      try {
        this.checkBindingsField(context, structure, field);
      } catch (final MiCheckerException e) {
        tracker.addException(e);
      }
    }

    tracker.throwIfNecessary();
  }

  private void checkBindingsField(
    final MiCheckerContext context,
    final MiASTStructure structure,
    final MiASTFieldType field)
    throws MiCheckerException
  {
    switch (field) {
      case final MiASTBitField bitField -> {
        this.checkBindingsBitField(bitField);
      }
      case final MiASTField plainField -> {
        this.checkBindingsPlainField(context, structure, plainField);
      }
    }
  }

  private void checkBindingsPlainField(
    final MiCheckerContext context,
    final MiASTStructure structure,
    final MiASTField field)
    throws MiCheckerException
  {
    final var type = field.type();
    checkBindingTypeReference(context, type);

    if (type.prefix().isEmpty()) {
      final var source = new TypePeer(structure.name().toSimpleName());
      final var target = new TypePeer(type.name().toSimpleName());
      try {
        this.graph.addEdge(
          source,
          target,
          new TypePeerReference(source, target)
        );
      } catch (final IllegalArgumentException e) {
        errorTypeCyclic(context, structure, source, target);
        throw new MiCheckerException();
      }
    }
  }

  private static void errorTypeCyclic(
    final MiCheckerContext context,
    final MiASTStructure structure,
    final TypePeer source,
    final TypePeer target)
  {
    final var attributes = new TreeMap<String, String>();
    context.putLexicalPosition(attributes, structure.lexical());

    context.error(
      new SStructuredError<>(
        "error-type-cyclic",
        context.format(ERROR_CHECKER_TYPE_REFERENCE_CYCLIC),
        attributes,
        Optional.empty(),
        Optional.empty()
      )
    );
  }

  private void checkBindingsBitField(
    final MiASTBitField bitField)
  {

  }

  private void checkBindingsTypeDeclaration(
    final MiASTScalarTypeDeclaration typeDeclaration)
  {

  }

  private static void checkBindingsMap(
    final MiCheckerContext context,
    final MiASTMap map)
    throws MiCheckerException
  {
    checkBindingTypeReference(context, map.type());
  }

  private static void checkBindingTypeReference(
    final MiCheckerContext context,
    final MiASTTypeReference type)
    throws MiCheckerException
  {
    final var prefixOpt = type.prefix();
    if (prefixOpt.isEmpty()) {
      final var targetName = type.name().toSimpleName();
      final var target = context.get(targetName);
      if (target == null) {
        errorTypeReferenceNonexistent(context, type);
        throw new MiCheckerException();
      }

      switch (target) {
        case final MiASTImportDeclaration ignored -> {
          throw new IllegalStateException();
        }
        case final MiASTMap map -> {
          errorTypeReferenceRefersToMap(context, type, map);
          throw new MiCheckerException();
        }
        case final MiASTScalarTypeDeclaration scalar -> {

        }
        case final MiASTStructure structure -> {

        }
      }
      return;
    }

    final var prefix =
      prefixOpt.get();
    final var packageTarget =
      context.packageForPrefix(prefix);
    final var typeTargetOpt =
      packageTarget.type(type.name().toSimpleName());

    if (typeTargetOpt.isEmpty()) {
      errorTypeReferenceNonexistent(context, type);
      throw new MiCheckerException();
    }
  }

  record TypePeer(
    MiSimpleName name)
  {

  }

  record TypePeerReference(
    TypePeer source,
    TypePeer target)
  {

  }

  private static void errorTypeReferenceRefersToMap(
    final MiCheckerContext context,
    final MiASTTypeReference type,
    final MiASTMap map)
  {
    final var attributes = new TreeMap<String, String>();
    context.putLexicalPosition(attributes, type.lexical());

    type.prefix().ifPresent(name -> {
      attributes.put(
        context.format(PREFIX),
        name.value()
      );
    });
    attributes.put(
      context.format(TYPE),
      type.name().value()
    );
    attributes.put(
      context.format(MAP),
      map.name().value()
    );

    context.error(
      new SStructuredError<>(
        "error-type-reference-map",
        context.format(ERROR_CHECKER_TYPE_REFERENCE_MAP),
        attributes,
        Optional.empty(),
        Optional.empty()
      )
    );
  }

  private static void errorTypeReferenceNonexistent(
    final MiCheckerContext context,
    final MiASTTypeReference type)
  {
    final var attributes = new TreeMap<String, String>();
    context.putLexicalPosition(attributes, type.lexical());

    type.prefix().ifPresent(name -> {
      attributes.put(
        context.format(PREFIX),
        name.value()
      );
    });
    attributes.put(
      context.format(TYPE),
      type.name().value()
    );

    context.error(
      new SStructuredError<>(
        "error-type-reference-nonexistent",
        context.format(ERROR_CHECKER_TYPE_REFERENCE_NONEXISTENT),
        attributes,
        Optional.empty(),
        Optional.empty()
      )
    );
  }
}
