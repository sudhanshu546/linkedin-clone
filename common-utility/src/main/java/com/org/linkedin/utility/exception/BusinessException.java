package com.org.linkedin.utility.exception;

public abstract class BusinessException extends RuntimeException {
  protected BusinessException(String message) {
    super(message);
  }

  public abstract int getStatusCode();
}
