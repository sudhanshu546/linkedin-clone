package com.org.linkedin.utility.exception;

public class ConflictException extends BusinessException {
  public ConflictException(String message) {
    super(message);
  }

  @Override
  public int getStatusCode() {
    return 409;
  }
}
