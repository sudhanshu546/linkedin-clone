package com.org.linkedin.utility.controller;

import com.org.linkedin.dto.BaseResponse;
import com.org.linkedin.utility.errors.ErrorKeys;
import com.org.linkedin.utility.exception.CommonExceptionHandler;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
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
   * Exception handler for CommonExceptionHandler.
   *
   * @param ex The CommonExceptionHandler instance.
   * @return ResponseEntity containing the error response.
   */
  @ExceptionHandler(CommonExceptionHandler.class)
  public ResponseEntity<BaseResponse<Void>> commonExceptionHandler(CommonExceptionHandler ex) {
    List<String> errors = new ArrayList<>();
    errors.add(ex.getMessage());
    BaseResponse<Void> returnValue =
        BaseResponse.<Void>builder().errorMessages(errors).status(ex.getStatusCode()).build();
    return new ResponseEntity<>(returnValue, new HttpHeaders(), ex.getStatusCode());
  }

  /**
   * Exception handler for ConnectException.
   *
   * @param connectException The ConnectException instance.
   * @return ResponseEntity containing the error response.
   */
  @ExceptionHandler(ConnectException.class)
  public ResponseEntity<BaseResponse<Void>> handleConnectException(
      ConnectException connectException) {
    List<String> errors = new ArrayList<>();
    errors.add(ErrorKeys.SERVICE_NOT_AVAILABLE);
    BaseResponse<Void> returnValue =
        BaseResponse.<Void>builder()
            .errorMessages(errors)
            .status(HttpStatus.SERVICE_UNAVAILABLE.value())
            .build();
    return new ResponseEntity<>(returnValue, new HttpHeaders(), HttpStatus.SERVICE_UNAVAILABLE);
  }

  /**
   * Exception handler for MethodArgumentNotValidException.
   *
   * @param ex The MethodArgumentNotValidException instance.
   * @return ResponseEntity containing the error response.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<BaseResponse<Void>> handleValidationErrors(
      MethodArgumentNotValidException ex) {
    List<String> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());
    BaseResponse<Void> returnValue =
        BaseResponse.<Void>builder()
            .errorMessages(errors)
            .status(HttpStatus.BAD_REQUEST.value())
            .build();

    return new ResponseEntity<>(returnValue, new HttpHeaders(), HttpStatus.BAD_REQUEST);
  }

  /**
   * Exception handler for HttpMessageNotReadableException.
   *
   * @param ex The HttpMessageNotReadableException instance.
   * @return ResponseEntity containing the error response.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<BaseResponse<Void>> handleInvalidHttpMessageExceptions(
      HttpMessageNotReadableException ex) {
    List<String> errors = new ArrayList<>();
    String invalidHttpMessageException = ErrorKeys.INVALID_REQUEST_BODY;
    errors.add(invalidHttpMessageException);
    BaseResponse<Void> returnValue =
        BaseResponse.<Void>builder()
            .errorMessages(errors)
            .status(HttpStatus.BAD_REQUEST.value())
            .build();
    return new ResponseEntity<>(returnValue, new HttpHeaders(), HttpStatus.BAD_REQUEST);
  }

  /**
   * Exception handler for generic Throwable.
   *
   * @param err The Throwable instance.
   * @return ResponseEntity containing the error response.
   */
  @ExceptionHandler(Throwable.class)
  public ResponseEntity<BaseResponse<Void>> handleGenericErrors(Throwable err) {
    log.error("Unknown error in handleGenericErrors method ", err);
    List<String> errors = new ArrayList<>();
    String errorMessage = ErrorKeys.SYSTEM_ERROR;
    errors.add(errorMessage);
    BaseResponse<Void> returnValue =
        BaseResponse.<Void>builder()
            .errorMessages(errors)
            .message(err.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .build();
    return new ResponseEntity<>(returnValue, new HttpHeaders(), HttpStatus.BAD_REQUEST);
  }

  /**
   * Exception handler for DataIntegrityViolationException.
   *
   * @param ex The DataIntegrityViolationException instance.
   * @return ResponseEntity containing the error response.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<String> handleValidationException(DataIntegrityViolationException ex) {
    String errorMessage = ex.getMessage();
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
  }
}
