package com.org.linkedin.user.config.keycloak;

import com.org.linkedin.dto.user.LoginRequest;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

public class UserLoginBuilder {

  public KeycloakBuilder keycloakLoginBuilder(KeycloakUserProperties keycloakUserProperties) {

    ResteasyClient resteasyClient =
        (ResteasyClient)
            ResteasyClientBuilder.newBuilder().register(new CustomObjectMapperProvider()).build();

    return KeycloakBuilder.builder()
        .serverUrl(keycloakUserProperties.getUrl())
        .realm(keycloakUserProperties.getRealm())
        .clientId(keycloakUserProperties.getClientId())
        .clientSecret(keycloakUserProperties.getClientSecret())
        .resteasyClient(resteasyClient);
  }

  public Keycloak keycloakLogin(KeycloakBuilder keycloakBuilder, LoginRequest request) {
    return keycloakBuilder.username(request.userName()).password(request.password()).build();
  }
}
