package com.org.linkedin.user.config.keycloak;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.stereotype.Component;

@Component
public class UserEntryBuilder {

  private ResteasyClient resteasyClient =
      (ResteasyClient)
          ResteasyClientBuilder.newBuilder().register(new CustomObjectMapperProvider()).build();

  public Keycloak keycloakBuilder(KeycloakUserProperties keycloakUserProperties) {
    return KeycloakBuilder.builder()
        .serverUrl(keycloakUserProperties.getUrl())
        .realm(keycloakUserProperties.getRealm())
        .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
        .clientId(keycloakUserProperties.getClientId())
        .clientSecret(keycloakUserProperties.getClientSecret())
        .resteasyClient(resteasyClient)
        .build();
  }
}
