package com.org.linkedin.user.config.keycloak.user;

import com.org.linkedin.user.config.keycloak.KeycloakUserProperties;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KeycloakDemoClientUserProperties extends KeycloakUserProperties {

  @Value("${keycloak.user.clientName}")
  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  @Value("${keycloak.user.clientSecret}")
  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }
}
