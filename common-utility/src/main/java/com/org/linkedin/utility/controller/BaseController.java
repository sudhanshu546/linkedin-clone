package com.org.linkedin.utility.controller;

import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.utility.exception.BusinessException;
import com.org.linkedin.utility.exception.CommonExceptionHandler;
import java.net.ConnectException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Controller advice to handle exceptions globally for the application. */
@Slf4j
@RestControllerAdvice
public class BaseController {

  /**
   * Exception handler for BusinessException.
   *
   * @param ex The BusinessException instance.
   * @return ResponseEntity containing the ApiResponse.
   */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
    return new ResponseEntity<>(
        ApiResponse.fail(ex.getMessage()), HttpStatus.valueOf(ex.getStatusCode()));
  }

  /**
   * Exception handler for CommonExceptionHandler (Legacy).
   *
   * @param ex The CommonExceptionHandler instance.
   * @return ResponseEntity containing the ApiResponse.
   */
  @ExceptionHandler(CommonExceptionHandler.class)
  public ResponseEntity<ApiResponse<Void>> handleCommonException(CommonExceptionHandler ex) {
    return new ResponseEntity<>(
        ApiResponse.fail(ex.getMessage()), HttpStatus.valueOf(ex.getStatusCode()));
  }

  /**
   * Exception handler for MethodArgumentNotValidException.
   *
   * @param ex The MethodArgumentNotValidException instance.
   * @return ResponseEntity containing the ApiResponse.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationErrors(
      MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
    return new ResponseEntity<>(ApiResponse.fail(message), HttpStatus.BAD_REQUEST);
  }

  /**
   * Exception handler for HttpMessageNotReadableException.
   *
   * @param ex The HttpMessageNotReadableException instance.
   * @return ResponseEntity containing the ApiResponse.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleInvalidHttpMessageExceptions(
      HttpMessageNotReadableException ex) {
    return new ResponseEntity<>(ApiResponse.fail("Invalid request body"), HttpStatus.BAD_REQUEST);
  }

  /**
   * Exception handler for DataIntegrityViolationException.
   *
   * @param ex The DataIntegrityViolationException instance.
   * @return ResponseEntity containing the ApiResponse.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
      DataIntegrityViolationException ex) {
    return new ResponseEntity<>(
        ApiResponse.fail("Data integrity violation: " + ex.getMostSpecificCause().getMessage()),
        HttpStatus.CONFLICT);
  }

  /**
   * Exception handler for ConnectException.
   *
   * @param ex The ConnectException instance.
   * @return ResponseEntity containing the ApiResponse.
   */
  @ExceptionHandler(ConnectException.class)
  public ResponseEntity<ApiResponse<Void>> handleConnectException(ConnectException ex) {
    return new ResponseEntity<>(
        ApiResponse.error("Service communication failure"), HttpStatus.SERVICE_UNAVAILABLE);
  }

  /**
   * Exception handler for generic Throwable.
   *
   * @param err The Throwable instance.
   * @return ResponseEntity containing the ApiResponse.
   */
  @ExceptionHandler(Throwable.class)
  public ResponseEntity<ApiResponse<Void>> handleThrowable(Throwable err) {
    log.error("Unhandled throwable: ", err);
    return new ResponseEntity<>(
        ApiResponse.error("An unexpected system error occurred. Please try again later."),
        HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
