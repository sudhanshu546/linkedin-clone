package com.org.linkedin.apigateway;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableDiscoveryClient
@OpenAPIDefinition(
    info =
        @Info(
            title = "API Gateway",
            version = "1.0",
            description = "Documentation API Gateway v1.0"),
    servers = {
      @Server(
          url = "${SWAGGER_SERVER_URL:http://localhost:8000/api}",
          description = "Deployment Server")
    })
public class ApiGatewayApplication {

  @Value("${app.cors.allowed-origins:/*,http://localhost:3000}")
  private List<String> allowedOrigins;

  public static void main(String[] args) {
    SpringApplication.run(ApiGatewayApplication.class, args);
  }

  @Bean
  public SecurityWebFilterChain configureResourceServer(ServerHttpSecurity serverHttpSecurity)
      throws Exception {
    return serverHttpSecurity
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(
            exchange ->
                exchange
                    .pathMatchers("/us/login/**")
                    .permitAll()
                    .pathMatchers("/us/user/add")
                    .permitAll()
                    .pathMatchers("/us/uploads/**")
                    .permitAll()
                    .anyExchange()
                    .authenticated())
        .oauth2ResourceServer((oauth) -> oauth.jwt(Customizer.withDefaults()))
        .build();
  }

  private CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedMethods(List.of("*"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
