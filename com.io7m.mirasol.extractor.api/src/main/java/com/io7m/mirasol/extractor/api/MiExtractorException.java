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


package com.io7m.mirasol.extractor.api;

import com.io7m.mirasol.core.MiException;
import com.io7m.seltzer.api.SStructuredErrorType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An exception raised during code extraction.
 */

public final class MiExtractorException
  extends MiException
{
  /**
   * The exception raised by the package.
   *
   * @param message             The error message
   * @param inErrorCode         The error code
   * @param inAttributes        The attributes
   * @param inRemediatingAction The remediating action
   * @param inExtras            The extras
   */

  public MiExtractorException(
    final String message,
    final String inErrorCode,
    final Map<String, String> inAttributes,
    final Optional<String> inRemediatingAction,
    final List<SStructuredErrorType<String>> inExtras)
  {
    super(message, inErrorCode, inAttributes, inRemediatingAction, inExtras);
  }

  /**
   * The exception raised by the package.
   *
   * @param message             The error message
   * @param cause               The cause
   * @param inErrorCode         The error code
   * @param inAttributes        The attributes
   * @param inRemediatingAction The remediating action
   * @param inExtras            The extras
   */

  public MiExtractorException(
    final String message,
    final Throwable cause,
    final String inErrorCode,
    final Map<String, String> inAttributes,
    final Optional<String> inRemediatingAction,
    final List<SStructuredErrorType<String>> inExtras)
  {
    super(
      message,
      cause,
      inErrorCode,
      inAttributes,
      inRemediatingAction,
      inExtras
    );
  }
}
