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

import com.io7m.mirasol.extractor.api.MiExtractorFactoryType;

/**
 * Machine-readable memory map documentation (Command-line)
 */

open module com.io7m.mirasol.cmdline
{
  requires com.io7m.mirasol.compiler;
  requires com.io7m.mirasol.core;
  requires com.io7m.mirasol.extractor.api;
  requires com.io7m.mirasol.loader.api;
  requires com.io7m.mirasol.strings;
  requires com.io7m.mirasol.compiler.api;

  requires com.io7m.jade.api;
  requires com.io7m.jade.vanilla;
  requires com.io7m.anethum.api;
  requires com.io7m.quarrel.core;
  requires com.io7m.quarrel.ext.logback;

  uses MiExtractorFactoryType;

  exports com.io7m.mirasol.cmdline;
}
