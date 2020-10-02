package de.enterprise.starters.kubernetes;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Loads default starter properties.
 *
 * @author Malte Geßner
 *
 */
@PropertySource("classpath:/META-INF/kubernetes-starter-default.properties")
@Configuration
public class DefaultPropertiesAutoConfiguration {

}
