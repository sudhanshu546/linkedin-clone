package com.org.linkedin.user.service;

import com.org.linkedin.dto.user.LoginRequest;
import org.keycloak.representations.AccessTokenResponse;

public interface LoginService {

  AccessTokenResponse login(LoginRequest loginRequest, String s);

  AccessTokenResponse getRefreshToken(String refreshToken, String s);

  //  AccessTokenResponse getToken(UUID userId, String clientName);
}
