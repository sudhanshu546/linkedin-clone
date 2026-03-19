package com.org.linkedin.apigateway.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

  @RequestMapping(value = "/user", method = {RequestMethod.GET, RequestMethod.POST})
  public Mono<Map<String, String>> userFallback() {
    return Mono.just(
        createFallbackResponse("User Service is temporarily unavailable. Please try again later."));
  }

  @RequestMapping(value = "/profile", method = {RequestMethod.GET, RequestMethod.POST})
  public Mono<Map<String, String>> profileFallback() {
    return Mono.just(
        createFallbackResponse(
            "Profile Service is temporarily unavailable. Please try again later."));
  }

  @RequestMapping(value = "/notification", method = {RequestMethod.GET, RequestMethod.POST})
  public Mono<Map<String, String>> notificationFallback() {
    return Mono.just(
        createFallbackResponse(
            "Notification Service is temporarily unavailable. Please try again later."));
  }

  @RequestMapping(value = "/job", method = {RequestMethod.GET, RequestMethod.POST})
  public Mono<Map<String, String>> jobFallback() {
    return Mono.just(
        createFallbackResponse("Job Service is temporarily unavailable. Please try again later."));
  }

  @RequestMapping(value = "/chat", method = {RequestMethod.GET, RequestMethod.POST})
  public Mono<Map<String, String>> chatFallback() {
    return Mono.just(
        createFallbackResponse("Chat Service is temporarily unavailable. Please try again later."));
  }

  private Map<String, String> createFallbackResponse(String message) {
    Map<String, String> response = new HashMap<>();
    response.put("status", "error");
    response.put("message", message);
    return response;
  }
}
