package com.org.linkedin.user.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuration class for setting up the database-related configurations.
 *
 * <p>This class is annotated with several Spring annotations to enable various features:
 *
 * <ul>
 *   <li>{@link Configuration}: Indicates that this class is a source of bean definitions.
 *   <li>{@link EnableJpaRepositories}: Enables JPA repositories and scans the specified package for
 *       repository interfaces.
 *   <li>{@link EnableJpaAuditing}: Enables JPA auditing and sets the auditor provider reference.
 *   <li>{@link EntityScan}: Configures the base packages used by the JPA entity scanner.
 *   <li>{@link EnableTransactionManagement}: Enables transaction management.
 * </ul>
 */
@Configuration
@EnableJpaRepositories({"com.hos.iot.user.repository"})
@EntityScan("com.hos.iot.domain")
@ComponentScan(basePackages = {"com.hos.iot.utility.controller", "com.hos.iot.utility.swagger"})
@EnableTransactionManagement
public class DatabaseConfiguration {}
