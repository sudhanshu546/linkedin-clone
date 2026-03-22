package com.org.linkedin.job;

import com.org.linkedin.utility.client.FeignAuthenticationInterceptor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableDiscoveryClient
@EntityScan(basePackages = {"com.org.linkedin.domain.job"})
@EnableJpaRepositories(basePackages = {"com.org.linkedin.job.repository"})
@EnableFeignClients(basePackages = "com.org.linkedin.utility.client")
@ComponentScan(
    basePackages = {"com.org.linkedin.utility", "com.org.linkedin.job"},
    basePackageClasses = FeignAuthenticationInterceptor.class)
public class JobServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(JobServiceApplication.class, args);
  }
}
