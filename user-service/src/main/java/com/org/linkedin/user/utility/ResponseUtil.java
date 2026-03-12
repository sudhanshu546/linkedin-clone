package com.org.linkedin.user.utility;

import com.org.linkedin.dto.BaseResponse;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/** Utility class for ResponseEntity creation. */
public interface ResponseUtil {

  /**
   * Wrap the optional into a {@link ResponseEntity} with an {@link HttpStatus#OK} status, or if
   * it's empty, it returns a {@link ResponseEntity} with {@link HttpStatus#NOT_FOUND}.
   *
   * @param <X> type of the response
   * @param maybeResponse response to return if present
   * @return response containing {@code maybeResponse} in BaseResponse if present or {@link
   *     HttpStatus#NOT_FOUND}
   */
  static <X> ResponseEntity<BaseResponse<X>> wrapWithBaseResponseOrNotFound(
      Optional<X> maybeResponse) {
    return wrapWithBaseResponseOrNotFound(maybeResponse, null);
  }

  /**
   * Wrap the optional into a {@link ResponseEntity} with an {@link HttpStatus#OK} status with the
   * headers, or if it's empty, throws a {@link ResponseStatusException} with status {@link
   * HttpStatus#NOT_FOUND}.
   *
   * @param <X> type of the response
   * @param maybeResponse response to return if present
   * @param header headers to be added to the response
   * @return response containing {@code maybeResponse} in BaseResponse if present
   */
  @SuppressWarnings("unchecked")
  static <X> ResponseEntity<BaseResponse<X>> wrapWithBaseResponseOrNotFound(
      Optional<X> maybeResponse, HttpHeaders header) {
    return (ResponseEntity<BaseResponse<X>>)
        maybeResponse
            .map(
                response ->
                    ResponseEntity.ok()
                        .headers(header)
                        .body(
                            BaseResponse.<X>builder()
                                .result(response)
                                .status(HttpStatus.OK.value())
                                .build()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  /**
   * Wraps an optional response in a {@link ResponseEntity} or throws a {@link
   * ResponseStatusException} with a {@link HttpStatus#NOT_FOUND} status if the response is not
   * present.
   *
   * <p>This method returns an {@link ResponseEntity} with the found response body if the {@link
   * Optional} is present, or throws a {@link ResponseStatusException} if the {@link Optional} is
   * empty.
   *
   * @param <X> the type of the response body
   * @param maybeResponse the optional response
   * @return the {@link ResponseEntity} containing the response body if present
   * @throws ResponseStatusException if the optional response is not present
   */
  static <X> ResponseEntity<X> wrapOrNotFound(Optional<X> maybeResponse) {
    return wrapOrNotFound(maybeResponse, null);
  }

  /**
   * Wraps an optional response in a {@link ResponseEntity} with the provided headers, or throws a
   * {@link ResponseStatusException} with a {@link HttpStatus#NOT_FOUND} status if the response is
   * not present.
   *
   * <p>This method returns an {@link ResponseEntity} with the found response body and the provided
   * headers if the {@link Optional} is present, or throws a {@link ResponseStatusException} if the
   * {@link Optional} is empty.
   *
   * @param <X> the type of the response body
   * @param maybeResponse the optional response
   * @param header the headers to be included in the {@link ResponseEntity}
   * @return the {@link ResponseEntity} containing the response body and headers if present
   * @throws ResponseStatusException if the optional response is not present
   */
  static <X> ResponseEntity<X> wrapOrNotFound(Optional<X> maybeResponse, HttpHeaders header) {
    return maybeResponse
        .map(response -> ResponseEntity.ok().headers(header).body(response))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }
}
