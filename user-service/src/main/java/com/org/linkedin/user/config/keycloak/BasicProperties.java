package com.org.linkedin.user.config.keycloak;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BasicProperties {

  @Value("${keycloak.realm}")
  String realm;

  @Value("${keycloak.url}")
  String url;
}
