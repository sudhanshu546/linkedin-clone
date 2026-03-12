package com.org.linkedin.user.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  @SuppressWarnings("unchecked")
  public AbstractAuthenticationToken convert(Jwt jwt) {
    Collection<GrantedAuthority> authorities = new ArrayList<>();

    // Extract permissions from authorization
    Map<String, Object> authorization = jwt.getClaimAsMap("authorization");
    if (authorization != null) {
      List<Map<String, Object>> permissions =
          (List<Map<String, Object>>) authorization.get("permissions");

      if (permissions != null) {
        permissions.forEach(
            permission -> {
              // Extract and add resource authority
              String resourceName = (String) permission.get("rsname");
              if (resourceName != null) {
                authorities.add(new SimpleGrantedAuthority(resourceName));
              }

              // Extract and add scope authorities
              List<String> scopes = (List<String>) permission.get("scopes");
              if (scopes != null) {
                scopes.forEach(scope -> authorities.add(new SimpleGrantedAuthority(scope)));
              }
            });
      }
    }

    // Extract client roles from "resource_access"
    Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
    if (resourceAccess != null) {
      for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
        Map<String, Object> clientData = (Map<String, Object>) entry.getValue();
        List<String> clientRoles = (List<String>) clientData.get("roles");

        if (clientRoles != null) {
          clientRoles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        }
      }
    }

    return new JwtAuthenticationToken(jwt, authorities);
  }
}
