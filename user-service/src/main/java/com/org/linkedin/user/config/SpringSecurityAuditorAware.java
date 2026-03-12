package com.org.linkedin.user.config;

import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Implementation of {@link AuditorAware} based on Spring Security. */
@Component
@AllArgsConstructor
public class SpringSecurityAuditorAware implements AuditorAware<UUID> {

  /**
   * Retrieves the current auditor's UUID from the Spring Security context.
   *
   * <p>This method gets the {@link Authentication} object from the {@link SecurityContextHolder}.
   * If the authentication is valid and the user is authenticated, it returns the user's UUID.
   *
   * @return an {@link Optional} containing the UUID of the current auditor, or an empty {@link
   *     Optional} if the user is not authenticated.
   */
  @Override
  public Optional<UUID> getCurrentAuditor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken)) {
      Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
      String subject = jwt.getSubject();
      // Start a new transaction to verify the existence in the database
      return Optional.of(UUID.fromString(subject));
    }
    return Optional.empty();
  }
}
