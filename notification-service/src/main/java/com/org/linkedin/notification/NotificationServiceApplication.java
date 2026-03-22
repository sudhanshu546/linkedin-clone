package com.org.linkedin.notification;

import com.org.linkedin.utility.client.FeignAuthenticationInterceptor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.org.linkedin.utility.client")
@ComponentScan(
    basePackages = {"com.org.linkedin.utility", "com.org.linkedin.notification"},
    basePackageClasses = FeignAuthenticationInterceptor.class)
public class NotificationServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(NotificationServiceApplication.class, args);
  }
}
