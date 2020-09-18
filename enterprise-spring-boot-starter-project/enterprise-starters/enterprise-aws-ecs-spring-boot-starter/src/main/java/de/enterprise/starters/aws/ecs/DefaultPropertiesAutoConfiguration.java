package de.enterprise.starters.aws.ecs;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Loads default starter properties.
 *
 * @author Malte Geßner
 *
 */
@PropertySource("classpath:/META-INF/aws-ecs-starter-default.properties")
@Configuration
public class DefaultPropertiesAutoConfiguration {

}
