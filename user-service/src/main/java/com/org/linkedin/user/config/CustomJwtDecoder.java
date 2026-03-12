package com.org.linkedin.user.config;

import static com.org.linkedin.constants.Constants.TOKEN_SUBJECT;

import com.org.linkedin.domain.user.TUser;
import com.org.linkedin.user.repository.UserRepository;
import com.org.linkedin.utility.errors.ErrorKeys;
import com.org.linkedin.utility.exception.CommonExceptionHandler;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@RequiredArgsConstructor
@Configuration
public class CustomJwtDecoder {

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String issuerUri;

  private final UserRepository userRepository;

  @Bean
  public JwtDecoder jwtDecoder() {
    NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
    MappedJwtClaimSetConverter converter =
        MappedJwtClaimSetConverter.withDefaults(
            Collections.singletonMap(TOKEN_SUBJECT, this::lookupUserIdBySub));
    jwtDecoder.setClaimSetConverter(converter);
    return jwtDecoder;
  }

  private String lookupUserIdBySub(Object subClaim) {
    return userRepository
        .findByKeycloakUserId(UUID.fromString((String) subClaim))
        .map(TUser::getId)
        .map(UUID::toString)
        .orElseThrow(
            () ->
                new CommonExceptionHandler(
                    ErrorKeys.ACCESS_DENIED, HttpStatus.UNAUTHORIZED.value()));
  }
}
