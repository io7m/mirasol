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


package com.io7m.mirasol.compiler.api;

import com.io7m.seltzer.api.SStructuredErrorType;

import java.util.List;
import java.util.Objects;

/**
 * The type of compilation results.
 *
 * @param <T> The type of returned values
 */

public sealed interface MiCompilerResultType<T>
{
  /**
   * Compilation succeeded.
   *
   * @param result The result
   * @param <T>    The result type
   */

  record Succeeded<T>(T result)
    implements MiCompilerResultType<T>
  {
    /**
     * Compilation succeeded.
     */

    public Succeeded
    {
      Objects.requireNonNull(result, "result");
    }
  }

  /**
   * Compilation failed.
   *
   * @param errors The errors
   * @param <T>    The result type
   */

  record Failed<T>(
    List<SStructuredErrorType<String>> errors)
    implements MiCompilerResultType<T>
  {
    /**
     * Compilation failed.
     */

    public Failed
    {
      Objects.requireNonNull(errors, "errors");
    }
  }
}
