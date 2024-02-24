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

import com.io7m.blackthorne.core.BTElementHandlerType;
import com.io7m.blackthorne.core.BTElementParsingContextType;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.lanark.core.RDottedName;
import com.io7m.mirasol.parser.api.ast.MiASTImportDeclaration;
import com.io7m.mirasol.parser.api.ast.MiASTPackageName;
import com.io7m.mirasol.parser.api.ast.MiASTSimpleName;
import org.xml.sax.Attributes;

import java.net.URI;

import static com.io7m.mirasol.parser.internal.Mi1.position;

/**
 * Element handler.
 */

public final class Mi1PImport
  implements BTElementHandlerType<Object, MiASTImportDeclaration>
{
  private LexicalPosition<URI> lexical;
  private MiASTSimpleName prefix;
  private MiASTPackageName packageName;

  /**
   * Element handler.
   *
   * @param context The parse context
   */

  public Mi1PImport(
    final BTElementParsingContextType context)
  {

  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
  {
    this.lexical =
      position(context.documentLocator());
    this.packageName =
      new MiASTPackageName(
        this.lexical,
        new RDottedName(attributes.getValue("Package"))
      );
    this.prefix =
      new MiASTSimpleName(this.lexical, attributes.getValue("As"));
  }

  @Override
  public MiASTImportDeclaration onElementFinished(
    final BTElementParsingContextType context)
  {
    return new MiASTImportDeclaration(
      this.lexical,
      this.packageName,
      this.prefix
    );
  }
}
