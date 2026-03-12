package com.org.linkedin.user.config.keycloak;

import static com.org.linkedin.user.constants.CustomOAuth2Constants.AUTHORIZATION;
import static com.org.linkedin.user.constants.CustomOAuth2Constants.BEARER;
import static com.org.linkedin.user.constants.CustomOAuth2Constants.CONTENT_TYPE;
import static com.org.linkedin.user.constants.CustomOAuth2Constants.CONTENT_TYPE_FORM_URLENCODED;
import static com.org.linkedin.user.constants.CustomOAuth2Constants.GRANT_TYPE_UMA_TICKET;
import static org.keycloak.OAuth2Constants.CLIENT_ID;
import static org.keycloak.OAuth2Constants.CLIENT_SECRET;
import static org.keycloak.OAuth2Constants.GRANT_TYPE;
import static org.keycloak.OAuth2Constants.REFRESH_TOKEN;

import lombok.RequiredArgsConstructor;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class KeyCloackHttpUtil {

  private final WebClient webClient;

  public AccessTokenResponse getPermissionToken(String accessToken, String client) {
    return webClient
        .post()
        .header(CONTENT_TYPE, CONTENT_TYPE_FORM_URLENCODED)
        .header(AUTHORIZATION, BEARER + accessToken)
        .bodyValue(GRANT_TYPE_UMA_TICKET + client)
        .retrieve()
        .bodyToMono(AccessTokenResponse.class)
        .block();
  }

  public AccessTokenResponse getTokenByRefreshToken(
      String refreshToken, String clientId, String clientSecret) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add(GRANT_TYPE, REFRESH_TOKEN);
    formData.add(REFRESH_TOKEN, refreshToken);
    formData.add(CLIENT_SECRET, clientSecret);
    formData.add(CLIENT_ID, clientId);

    return webClient
        .post()
        .header(CONTENT_TYPE, CONTENT_TYPE_FORM_URLENCODED)
        .body(BodyInserters.fromFormData(formData))
        .retrieve()
        .bodyToMono(AccessTokenResponse.class)
        .block();
  }
}
