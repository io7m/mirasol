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


package com.io7m.mirasol.parser;

import com.io7m.mirasol.parser.api.MiSerializerFactoryType;
import com.io7m.mirasol.parser.api.MiSerializerOptions;
import com.io7m.mirasol.parser.api.MiSerializerType;
import com.io7m.mirasol.parser.internal.MiSerializer;

import java.io.OutputStream;
import java.net.URI;
import java.util.Objects;

/**
 * The default package serializers.
 */

public final class MiSerializers
  implements MiSerializerFactoryType
{
  /**
   * The default package serializers.
   */

  public MiSerializers()
  {

  }

  @Override
  public MiSerializerType createSerializerWithContext(
    final MiSerializerOptions context,
    final URI target,
    final OutputStream stream)
  {
    return new MiSerializer(
      Objects.requireNonNullElse(context, MiSerializerOptions.defaultOptions()),
      Objects.requireNonNull(target, "target"),
      Objects.requireNonNull(stream, "stream")
    );
  }
}
