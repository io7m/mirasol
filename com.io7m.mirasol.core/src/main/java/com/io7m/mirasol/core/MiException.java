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


package com.io7m.mirasol.core;

import com.io7m.seltzer.api.SStructuredErrorExceptionType;
import com.io7m.seltzer.api.SStructuredErrorType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The exception raised by the package.
 */

public final class MiException
  extends Exception
  implements SStructuredErrorExceptionType<String>
{
  private final String errorCode;
  private final Map<String, String> attributes;
  private final Optional<String> remediatingAction;
  private final List<SStructuredErrorType<String>> extras;

  /**
   * @return The extra errors associated with this exception
   */

  public List<SStructuredErrorType<String>> extras()
  {
    return this.extras;
  }

  /**
   * The exception raised by the package.
   *
   * @param message             The error message
   * @param inErrorCode         The error code
   * @param inAttributes        The attributes
   * @param inRemediatingAction The remediating action
   * @param inExtras            The extras
   */

  public MiException(
    final String message,
    final String inErrorCode,
    final Map<String, String> inAttributes,
    final Optional<String> inRemediatingAction,
    final List<SStructuredErrorType<String>> inExtras)
  {
    super(Objects.requireNonNull(message, "message"));

    this.errorCode =
      Objects.requireNonNull(inErrorCode, "errorCode");
    this.attributes =
      Map.copyOf(inAttributes);
    this.remediatingAction =
      Objects.requireNonNull(inRemediatingAction, "remediatingAction");
    this.extras =
      List.copyOf(inExtras);
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

  public MiException(
    final String message,
    final Throwable cause,
    final String inErrorCode,
    final Map<String, String> inAttributes,
    final Optional<String> inRemediatingAction,
    final List<SStructuredErrorType<String>> inExtras)
  {
    super(
      Objects.requireNonNull(message, "message"),
      Objects.requireNonNull(cause, "cause")
    );

    this.errorCode =
      Objects.requireNonNull(inErrorCode, "errorCode");
    this.attributes =
      Map.copyOf(inAttributes);
    this.remediatingAction =
      Objects.requireNonNull(inRemediatingAction, "remediatingAction");
    this.extras =
      List.copyOf(inExtras);
  }

  @Override
  public String errorCode()
  {
    return this.errorCode;
  }

  @Override
  public Map<String, String> attributes()
  {
    return this.attributes;
  }

  @Override
  public Optional<String> remediatingAction()
  {
    return this.remediatingAction;
  }

  @Override
  public Optional<Throwable> exception()
  {
    return Optional.of(this);
  }
}