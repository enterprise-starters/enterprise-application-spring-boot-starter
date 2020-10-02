package de.enterprise.starters.jpa;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Loads default starter properties.
 *
 * @author Malte Geßner
 *
 */
@PropertySource("classpath:/META-INF/jpa-starter-default.properties")
@Configuration
public class DefaultPropertiesAutoConfiguration {

}
