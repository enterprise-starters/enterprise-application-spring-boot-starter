package de.enterprise.starters.azure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Jonas Keßler
 */
@Configuration
@EnableConfigurationProperties(AzureLoggingProperties.class)
public class AzureAutoConfiguration {

}
