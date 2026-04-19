package com.org.linkedin.apigateway.controller;

import com.org.linkedin.dto.ApiResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

  @RequestMapping(
      value = "/user",
      method = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.PUT,
        RequestMethod.DELETE,
        RequestMethod.PATCH
      })
  public Mono<ApiResponse<Void>> userFallback() {
    return Mono.just(
        ApiResponse.error("User Service is temporarily unavailable. Please try again later."));
  }

  @RequestMapping(
      value = "/profile",
      method = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.PUT,
        RequestMethod.DELETE,
        RequestMethod.PATCH
      })
  public Mono<ApiResponse<Void>> profileFallback() {
    return Mono.just(
        ApiResponse.error("Profile Service is temporarily unavailable. Please try again later."));
  }

  @RequestMapping(
      value = "/notification",
      method = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.PUT,
        RequestMethod.DELETE,
        RequestMethod.PATCH
      })
  public Mono<ApiResponse<Void>> notificationFallback() {
    return Mono.just(
        ApiResponse.error(
            "Notification Service is temporarily unavailable. Please try again later."));
  }

  @RequestMapping(
      value = "/job",
      method = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.PUT,
        RequestMethod.DELETE,
        RequestMethod.PATCH
      })
  public Mono<ApiResponse<Void>> jobFallback() {
    return Mono.just(
        ApiResponse.error("Job Service is temporarily unavailable. Please try again later."));
  }

  @RequestMapping(
      value = "/chat",
      method = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.PUT,
        RequestMethod.DELETE,
        RequestMethod.PATCH
      })
  public Mono<ApiResponse<Void>> chatFallback() {
    return Mono.just(
        ApiResponse.error("Chat Service is temporarily unavailable. Please try again later."));
  }

  @RequestMapping(
      value = "/search",
      method = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.PUT,
        RequestMethod.DELETE,
        RequestMethod.PATCH
      })
  public Mono<ApiResponse<Void>> searchFallback() {
    return Mono.just(
        ApiResponse.error("Search Service is temporarily unavailable. Please try again later."));
  }
}
