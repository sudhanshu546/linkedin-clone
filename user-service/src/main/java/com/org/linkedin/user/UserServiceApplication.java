package com.org.linkedin.user;

import static com.org.linkedin.constants.Constants.SERVICE_TIMEZONE;

import com.org.linkedin.user.config.CustomAuthenticationConverter;
import com.org.linkedin.utility.client.FeignAuthenticationInterceptor;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@EnableWebSecurity
@EnableAsync
@EnableJpaAuditing
// @EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@EnableDiscoveryClient
@OpenAPIDefinition(
    info =
        @Info(
            title = "User Service",
            version = "1.0",
            description = "Documentation User Service v1.0"))
@ComponentScan(
    basePackages = {"com.org.linkedin.utility", "com.org.linkedin.user"},
    basePackageClasses = FeignAuthenticationInterceptor.class)
@EnableFeignClients("com.org.linkedin.utility.client")
@EnableJpaRepositories(basePackages = "com.org.linkedin.user.repository")
@EntityScan(basePackages = {"com.org.linkedin.domain", "com.org.linkedin.user.domain"})
public class UserServiceApplication {

  @Value("${keycloak.baseurl}")
  private String baseurl;

  private final JwtDecoder customJwtDecoder;

  /**
   * The entry point for the UserServiceApplication. This method launches the Spring Boot
   * application.
   *
   * @param args command line arguments passed to the application.
   */
  public static void main(String[] args) {
    SpringApplication.run(UserServiceApplication.class, args);
  }

  /**
   * Initializes the application by setting the default time zone to UTC. This method is called
   * after the bean's properties have been set.
   */
  @PostConstruct
  public void init() {
    TimeZone.setDefault(TimeZone.getTimeZone(SERVICE_TIMEZONE));
  }

  /**
   * Creates and configures a WebClient with a base URL. This WebClient can be used for making HTTP
   * requests.
   *
   * @return a configured WebClient instance.
   */
  @Bean
  public WebClient webClient() {
    return WebClient.builder().baseUrl(baseurl).build();
  }

  /**
   * Configures the security filter chain for the application. This method sets up CSRF protection
   * to be disabled and configures the security rules for various API endpoints. It allows
   * unauthenticated access to specific paths such as login, registration, and other public
   * endpoints. All other requests require authentication. Additionally, it configures OAuth2
   * resource server with JWT token validation.
   *
   * @param http the {@link HttpSecurity} to configure.
   * @return the configured {@link SecurityFilterChain}.
   * @throws Exception if an error occurs during the configuration.
   */
  @Bean
  public SecurityFilterChain configure(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        new AntPathRequestMatcher("/us/user/add"),
                        new AntPathRequestMatcher("/us/login/**"))
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(
                    jwt ->
                        jwt.jwtAuthenticationConverter(new CustomAuthenticationConverter())
                            .decoder(customJwtDecoder)));
    return http.build();
  }
}
