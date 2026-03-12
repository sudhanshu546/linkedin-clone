package com.org.linkedin.user.config.keycloak.user;

import com.org.linkedin.dto.user.LoginRequest;
import com.org.linkedin.user.config.keycloak.UserLoginBuilder;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KeycloakDemoClientUserInstanceBuilder extends UserLoginBuilder {

  private final KeycloakDemoClientUserProperties keycloakDemoClientUserProperties;

  @Bean("keycloakStaffBuilder")
  public KeycloakBuilder keycloakDemoClientBuilder() {
    return super.keycloakLoginBuilder(keycloakDemoClientUserProperties);
  }

  public Keycloak keycloakDemoClientUser(LoginRequest loginRequest) {
    return super.keycloakLogin(keycloakDemoClientBuilder(), loginRequest);
  }
}
