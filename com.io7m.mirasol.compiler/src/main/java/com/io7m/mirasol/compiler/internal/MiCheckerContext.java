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

import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.mirasol.core.MiException;
import com.io7m.mirasol.core.MiMapType;
import com.io7m.mirasol.core.MiPackageElementType;
import com.io7m.mirasol.core.MiPackageName;
import com.io7m.mirasol.core.MiPackageReference;
import com.io7m.mirasol.core.MiPackageType;
import com.io7m.mirasol.core.MiScalarType;
import com.io7m.mirasol.core.MiSimpleName;
import com.io7m.mirasol.core.MiSizeOctets;
import com.io7m.mirasol.core.MiStructureType;
import com.io7m.mirasol.loader.api.MiLoaderType;
import com.io7m.mirasol.parser.api.ast.MiASTImportDeclaration;
import com.io7m.mirasol.parser.api.ast.MiASTMap;
import com.io7m.mirasol.parser.api.ast.MiASTPackageDeclaration;
import com.io7m.mirasol.parser.api.ast.MiASTPackageElementType;
import com.io7m.mirasol.parser.api.ast.MiASTScalarTypeDeclaration;
import com.io7m.mirasol.parser.api.ast.MiASTSimpleName;
import com.io7m.mirasol.parser.api.ast.MiASTStructure;
import com.io7m.mirasol.strings.MiStringConstantType;
import com.io7m.mirasol.strings.MiStrings;
import com.io7m.seltzer.api.SStructuredError;
import com.io7m.seltzer.api.SStructuredErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import static com.io7m.mirasol.strings.MiStringConstants.COLUMN;
import static com.io7m.mirasol.strings.MiStringConstants.ERROR_CHECKER_IMPORT_MISSING;
import static com.io7m.mirasol.strings.MiStringConstants.FILE;
import static com.io7m.mirasol.strings.MiStringConstants.LINE;
import static com.io7m.mirasol.strings.MiStringConstants.PREFIX;

final class MiCheckerContext
{
  private static final Logger LOG =
    LoggerFactory.getLogger(MiCheckerPassSizes.class);

  private final MiASTPackageDeclaration source;
  private final ArrayList<SStructuredErrorType<String>> errors;
  private final MiStrings strings;
  private final MiLoaderType loader;
  private final HashMap<MiSimpleName, MiPackageType> importedPackages;
  private final HashMap<MiSimpleName, MiASTPackageElementType> elementsByName;
  private final HashMap<MiSimpleName, MiSizeOctets> sizesInOctets;
  private final HashMap<MiSimpleName, MiPackageElementType> buildElements;

  /**
   * A binding and type checker.
   *
   * @param inStrings The strings
   * @param inLoader  The loader
   * @param inSource  The source
   */

  MiCheckerContext(
    final MiStrings inStrings,
    final MiLoaderType inLoader,
    final MiASTPackageDeclaration inSource)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "inStrings");
    this.loader =
      Objects.requireNonNull(inLoader, "inLoader");
    this.source =
      Objects.requireNonNull(inSource, "source");
    this.errors =
      new ArrayList<>();
    this.importedPackages =
      new HashMap<>();
    this.elementsByName =
      new HashMap<>();
    this.sizesInOctets =
      new HashMap<>();
    this.buildElements =
      new HashMap<>();
  }

  public MiCheckerException error(
    final SStructuredErrorType<String> error)
  {
    this.errors.add(error);
    return new MiCheckerException();
  }

  public MiPackageType openPackage(
    final MiPackageName name)
    throws MiException
  {
    return this.loader.openPackage(name);
  }

  public void addImport(
    final MiASTSimpleName prefix,
    final MiPackageType pack)
  {
    this.importedPackages.put(prefix.toSimpleName(), pack);
  }

  public MiASTPackageDeclaration source()
  {
    return this.source;
  }

  public void bind(
    final MiASTPackageElementType element)
  {
    switch (element) {
      case final MiASTImportDeclaration importDeclaration -> {
        throw new IllegalArgumentException("Cannot bind an import");
      }
      case final MiASTMap map -> {
        this.elementsByName.put(
          map.name().toSimpleName(),
          map
        );
      }
      case final MiASTScalarTypeDeclaration typeDeclaration -> {
        this.elementsByName.put(
          typeDeclaration.name().toSimpleName(),
          typeDeclaration
        );
      }
      case final MiASTStructure astStructure -> {
        this.elementsByName.put(
          astStructure.name().toSimpleName(),
          astStructure
        );
      }
    }
  }

  public MiASTPackageElementType get(
    final MiSimpleName targetName)
  {
    return this.elementsByName.get(targetName);
  }

  public String format(
    final MiStringConstantType constant,
    final Object... arguments)
  {
    return this.strings.format(constant, arguments);
  }

  public MiPackageType packageForPrefix(
    final MiASTSimpleName prefix)
    throws MiCheckerException
  {
    final var packageTarget =
      this.importedPackages.get(prefix.toSimpleName());

    if (packageTarget == null) {
      this.errorNonexistentTypePrefix(prefix);
      throw new MiCheckerException();
    }

    return packageTarget;
  }

  public void errorNonexistentTypePrefix(
    final MiASTSimpleName prefix)
  {
    final var attributes = new TreeMap<String, String>();
    this.putLexicalPosition(attributes, prefix.lexical());

    attributes.put(
      this.strings.format(PREFIX),
      prefix.value()
    );

    this.errors.add(
      new SStructuredError<>(
        "error-import-missing",
        this.strings.format(ERROR_CHECKER_IMPORT_MISSING),
        attributes,
        Optional.empty(),
        Optional.empty()
      )
    );
  }

  public void putLexicalPosition(
    final Map<String, String> attributes,
    final LexicalPosition<URI> lexical)
  {
    attributes.put(
      this.format(LINE),
      Integer.toUnsignedString(lexical.line())
    );
    attributes.put(
      this.format(COLUMN),
      Integer.toUnsignedString(lexical.column())
    );
    attributes.put(
      this.format(FILE),
      lexical.file().map(URI::toString).orElse("<unavailable>")
    );
  }

  public List<SStructuredErrorType<String>> errors()
  {
    return List.copyOf(this.errors);
  }

  public MiPackageType createPackage()
  {
    final var output =
      new MiPackage(this.source.name().toPackageName());

    output.setDocumentation(this.source.documentation().value());

    {
      final var entries =
        this.importedPackages.entrySet()
          .stream()
          .sorted(Map.Entry.comparingByKey())
          .toList();

      for (final var entry : entries) {
        output.addImport(new MiPackageReference(
          entry.getValue().name(),
          entry.getKey()
        ));
      }
    }

    for (final var entry : this.buildElements.entrySet()) {
      switch (entry.getValue()) {
        case final MiMapType map -> {
          output.addMap(map);
        }
        case final MiScalarType sc -> {
          output.addType(sc);
        }
        case final MiStructureType st -> {
          output.addType(st);
        }
      }
    }

    return output;
  }

  public Optional<MiSizeOctets> sizeOf(
    final MiSimpleName name)
  {
    return Optional.ofNullable(this.sizesInOctets.get(name));
  }

  public void sizeSave(
    final MiSimpleName name,
    final MiSizeOctets sizeOctets)
  {
    LOG.trace("Size {} -> {}", name, sizeOctets);
    this.sizesInOctets.put(name, sizeOctets);
  }

  public void buildSave(
    final MiPackageElementType element)
  {
    switch (element) {
      case final MiScalarType sc -> {
        this.buildElements.put(sc.name(), sc);
      }
      case final MiStructureType st -> {
        this.buildElements.put(st.name(), st);
      }
      case final MiMapType m -> {
        this.buildElements.put(m.name(), m);
      }
    }
  }

  public MiPackageElementType buildGet(
    final MiSimpleName name)
  {
    return this.buildElements.get(name);
  }
}
