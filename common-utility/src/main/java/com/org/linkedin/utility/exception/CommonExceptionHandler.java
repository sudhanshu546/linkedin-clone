package com.org.linkedin.utility.exception;

import lombok.Getter;

/** Exception class for handling common exceptions. */
@Getter
public class CommonExceptionHandler extends RuntimeException {

  /** The status code associated with the exception. */
  private final int statusCode;

  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new CommonExceptionHandler with the specified message and status code.
   *
   * @param message The detail message.
   * @param statusCode The HTTP status code associated with the exception.
   */
  public CommonExceptionHandler(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }
}
