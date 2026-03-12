package com.org.linkedin.user.config.keycloak;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class KeycloakClients {

  @Value("${keycloak.user.clientName}")
  private String clientName;

  @Value("${keycloak.user.clientId}")
  private String clientId;

  public String clientName() {
    return clientName;
  }

  public String clientId() {
    return clientId;
  }
}
