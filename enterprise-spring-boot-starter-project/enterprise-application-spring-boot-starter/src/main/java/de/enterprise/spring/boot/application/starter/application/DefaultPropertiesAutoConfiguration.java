package de.enterprise.spring.boot.application.starter.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Loads default starter properties.
 *
 * @author Malte Geßner
 *
 */
@PropertySource("classpath:/META-INF/application-enterprise-starter.properties")
@Configuration
@EnableConfigurationProperties(EnterpriseApplicationProperties.class)
public class DefaultPropertiesAutoConfiguration {

}
