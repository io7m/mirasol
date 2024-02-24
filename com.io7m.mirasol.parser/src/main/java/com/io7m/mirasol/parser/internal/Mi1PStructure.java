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
import com.io7m.mirasol.parser.api.ast.MiASTDocumentation;
import com.io7m.mirasol.parser.api.ast.MiASTFieldType;
import com.io7m.mirasol.parser.api.ast.MiASTSimpleName;
import com.io7m.mirasol.parser.api.ast.MiASTStructure;
import org.xml.sax.Attributes;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import static com.io7m.mirasol.parser.internal.Mi1.position;

/**
 * Element handler.
 */

public final class Mi1PStructure
  implements BTElementHandlerType<Object, MiASTStructure>
{
  private final ArrayList<MiASTFieldType> fields;
  private MiASTSimpleName name;
  private LexicalPosition<URI> lexical;
  private MiASTDocumentation documentation;

  /**
   * Element handler.
   *
   * @param context The parse context
   */

  public Mi1PStructure(
    final BTElementParsingContextType context)
  {
    this.fields =
      new ArrayList<>();
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
        Mi1.element("Documentation"),
        Mi1PDocumentation::new
      ),
      Map.entry(
        Mi1.element("Field"),
        Mi1PField::new
      ),
      Map.entry(
        Mi1.element("BitField"),
        Mi1PBitField::new
      )
    );
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
      case final MiASTFieldType o -> {
        this.fields.add(o);
      }
      default -> {
        throw new IllegalStateException(
          "Unexpected value: %s".formatted(result));
      }
    }
  }

  @Override
  public MiASTStructure onElementFinished(
    final BTElementParsingContextType context)
    throws Exception
  {
    return new MiASTStructure(
      this.lexical,
      this.documentation,
      this.name,
      this.fields
    );
  }
}
