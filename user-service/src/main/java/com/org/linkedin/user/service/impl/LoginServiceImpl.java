package com.org.linkedin.user.service.impl;

import com.org.linkedin.domain.user.TUser;
import com.org.linkedin.dto.user.LoginRequest;
import com.org.linkedin.user.config.keycloak.BasicProperties;
import com.org.linkedin.user.config.keycloak.KeyCloackHttpUtil;
import com.org.linkedin.user.config.keycloak.KeycloakClients;
import com.org.linkedin.user.config.keycloak.user.KeycloakDemoClientUserInstanceBuilder;
import com.org.linkedin.user.config.keycloak.user.KeycloakDemoClientUserProperties;
import com.org.linkedin.user.repository.UserRepository;
import com.org.linkedin.user.service.LoginService;
import com.org.linkedin.utility.errors.ErrorKeys;
import com.org.linkedin.utility.exception.CommonExceptionHandler;
import jakarta.ws.rs.NotAuthorizedException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginServiceImpl implements LoginService {

  private final KeyCloackHttpUtil tbsHttpUtil;

  private final KeycloakDemoClientUserProperties keycloakDemoClientUserProperties;
  private final KeycloakDemoClientUserInstanceBuilder keycloakDemoClientUserInstanceBuilder;
  private final UserRepository userRepository;
  private final KeycloakClients keycloakClients;
  private final BasicProperties basicProperties;

  @Value("${keycloak.user.clientSecret}")
  String clientSecret;

  /**
   * Attempts to log in a user or user based on the provided login request and chain. . It fetches
   * user details from the appropriate repository, checks if the user is active, and then attempts
   * to get an access token using Keycloak. If successful, it retrieves a permission token.
   *
   * @param loginRequest the login credentials including username and password.
   * @param chain the client chain to determine if the login is for a normal user or a staff user.
   * @return AccessTokenResponse containing the permission token.
   * @throws "CommonExceptionHandler" with different error keys based on the error scenario (e.g.,
   *     invalid credentials, user not found, etc.).
   */
  @Override
  public AccessTokenResponse login(LoginRequest loginRequest, String chain) {
    try {
      log.debug("Enter in login method :: loginRequest [{}] :: chain [{}]", loginRequest, chain);
      String username = loginRequest.userName().toLowerCase();
      log.debug("username is :: [{}]", username);
      Optional<TUser> userOptional;
      if (keycloakClients.clientName().equalsIgnoreCase(chain)) {
        userOptional = userRepository.findByEmail(username);
      } else {
        throw new CommonExceptionHandler(ErrorKeys.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value());
      }

      TUser user =
          userOptional.orElseThrow(
              () ->
                  new CommonExceptionHandler(
                      ErrorKeys.USER_NOT_FOUND, HttpStatus.BAD_REQUEST.value()));
      log.debug("User details is :: [{}]", user);
      if (!user.getIsEnabled()) {
        throw new CommonExceptionHandler(ErrorKeys.USER_INACTIVE, HttpStatus.BAD_REQUEST.value());
      }
      AccessTokenResponse res =
          keycloakDemoClientUserInstanceBuilder
              .keycloakDemoClientUser(loginRequest)
              .tokenManager()
              .getAccessToken();
      log.debug("Exit in login method :: AccessTokenResponse [{}]", res);
      //      return tbsHttpUtil.getPermissionToken(res.getToken(), chain);
      return res;
    } catch (NotAuthorizedException e) {
      log.debug("Invalid User Credentials in Login Request :: [{}] ", loginRequest);
      throw new CommonExceptionHandler(
          ErrorKeys.INVALID_USER_CREDENTIALS, HttpStatus.BAD_REQUEST.value());
    } catch (WebClientResponseException.Forbidden ex) {
      log.error("Error in the login method ", ex);
      log.debug("Access Denied in Login Request :: [{}] ", loginRequest);
      throw new CommonExceptionHandler(ErrorKeys.ACCESS_DENIED, HttpStatus.BAD_REQUEST.value());
    }
  }

  /**
   * Retrieves a refreshed access token using the provided refresh token and client chain. This
   * method determines the client secret based on the chain and uses it to request a new access
   * token. If the token retrieval is successful, it also fetches a permission token using the new
   * access token.
   *
   * @param refreshToken the refresh token used to request a new access token.
   * @param chain the client chain (either staff or normal client) to determine the appropriate
   *     client secret.
   * @return AccessTokenResponse containing the new access token and permission token.
   * @throws CommonExceptionHandler if the refresh token is invalid or if there is an issue with the
   *     web client.
   */
  @Override
  public AccessTokenResponse getRefreshToken(String refreshToken, String chain) {
    log.debug("Enter in get refresh token :: refreshToken [{}] :: chain [{}]", refreshToken, chain);
    try {
      String clientSecret =
          chain.equalsIgnoreCase(keycloakClients.clientName())
              ? keycloakDemoClientUserProperties.getClientSecret()
              : null;
      log.debug("client secret is :: [{}]", clientSecret);
      AccessTokenResponse res =
          tbsHttpUtil.getTokenByRefreshToken(refreshToken, chain, clientSecret);
      log.debug("Access token response is :: [{}]", res);
      log.debug("Exit in get refresh token method .");
      // Only get the permission token if the initial token retrieval succeeds
      return tbsHttpUtil.getPermissionToken(res.getToken(), chain);
    } catch (WebClientResponseException e) {
      log.error("Invalid Token in Token Refresh Request", e);
      throw new CommonExceptionHandler(
          ErrorKeys.INVALID_REFRESH_TOKEN, HttpStatus.BAD_REQUEST.value());
    }
  }

  /**
   * Retrieves an access token for a specified user by decoding their stored password. This method
   * fetches a user from the database using their UUID, decodes the user's password from Base64, and
   * generates a new access token using the user's email and decoded password.
   *
   * @param userId the UUID of the user for whom to obtain an access token
   * @param clientName the name of the client requesting the token
   * @return an {@link AccessTokenResponse} containing the new access token for the user
   */
  //  @Override
  //  public AccessTokenResponse getToken(UUID userId, String clientName) {
  //    TUser user = userRepository.findById(userId).get();
  //    return login(
  //        new LoginRequest(
  //            user.getEmail(), new String(Base64.getDecoder().decode(user.getPassword()))),
  //        clientName);
  //  }
}
