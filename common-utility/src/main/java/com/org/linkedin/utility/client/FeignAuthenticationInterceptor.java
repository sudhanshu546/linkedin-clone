package com.org.linkedin.utility.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * A Feign request interceptor that adds the OAuth2 JWT token to the Authorization header of
 * outgoing requests.
 */
@Component
public class FeignAuthenticationInterceptor implements RequestInterceptor {

  /**
   * Adds the Authorization header to outgoing Feign requests, except for internal URLs.
   *
   * @param template the request template to which the Authorization header will be added.
   */
  @Override
  public void apply(RequestTemplate template) {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String interceptedUrl = template.url();
    if (interceptedUrl.contains("internal")) return;

    if (authentication instanceof JwtAuthenticationToken) {
      JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
      String accessToken = token.getToken().getTokenValue();
      template.header("Authorization", "Bearer " + accessToken);
    } else {
      throw new UnsupportedOperationException(
          "Unsupported authentication token type: " + authentication.getClass().getName());
    }
  }
}
