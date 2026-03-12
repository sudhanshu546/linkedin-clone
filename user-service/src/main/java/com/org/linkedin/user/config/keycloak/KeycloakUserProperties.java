package com.org.linkedin.user.config.keycloak;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@EqualsAndHashCode(callSuper = false)
@FieldDefaults(level = AccessLevel.PUBLIC)
public class KeycloakUserProperties extends BasicProperties {

  String clientId;
  String clientSecret;
}
