package com.org.linkedin.utility.exception;

public class ValidationException extends BusinessException {
  public ValidationException(String message) {
    super(message);
  }

  @Override
  public int getStatusCode() {
    return 400;
  }
}
