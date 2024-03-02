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

import com.io7m.blackthorne.core.BTElementHandlerConstructorType;
import com.io7m.blackthorne.core.BTElementHandlerType;
import com.io7m.blackthorne.core.BTElementParsingContextType;
import com.io7m.blackthorne.core.BTQualifiedName;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.mirasol.core.MiSizeBits;
import com.io7m.mirasol.parser.api.ast.MiASTDocumentation;
import com.io7m.mirasol.parser.api.ast.MiASTScalarTypeDeclaration;
import com.io7m.mirasol.parser.api.ast.MiASTSimpleName;
import org.xml.sax.Attributes;

import java.math.BigInteger;
import java.net.URI;
import java.util.Map;

import static com.io7m.mirasol.parser.internal.Mi1.element;
import static com.io7m.mirasol.parser.internal.Mi1.position;

/**
 * Element handler.
 */

public final class Mi1PScalarType
  implements BTElementHandlerType<Object, MiASTScalarTypeDeclaration>
{
  private MiASTDocumentation documentation;
  private LexicalPosition<URI> lexical;
  private MiASTSimpleName name;
  private MiASTSimpleName kind;
  private MiSizeBits size;

  /**
   * Element handler.
   *
   * @param context The parse context
   */

  public Mi1PScalarType(
    final BTElementParsingContextType context)
  {
    this.documentation =
      MiASTDocumentation.none();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(
        element("Documentation"),
        Mi1PDocumentation::new
      )
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
  {
    switch (result) {
      case final MiASTDocumentation d -> {
        this.documentation = d;
      }
      default -> {
        throw new IllegalStateException(
          "Unexpected value: %s".formatted(result)
        );
      }
    }
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
  {
    this.lexical =
      position(context.documentLocator());
    this.name =
      new MiASTSimpleName(this.lexical, attributes.getValue("Name"));
    this.kind =
      new MiASTSimpleName(this.lexical, attributes.getValue("Kind"));
    this.size =
      new MiSizeBits(new BigInteger(attributes.getValue("SizeInBits")));
  }

  @Override
  public MiASTScalarTypeDeclaration onElementFinished(
    final BTElementParsingContextType context)
  {
    return new MiASTScalarTypeDeclaration(
      this.lexical,
      this.documentation,
      this.name,
      this.kind,
      this.size
    );
  }
}
