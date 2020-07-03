package de.enterprise.spring.boot.application.starter.actuate;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import de.enterprise.spring.boot.application.starter.actuate.endpoint.DocumentationMvcEndpoint;
import de.enterprise.spring.boot.application.starter.actuate.reactive.ReactiveActuatorSecurityConfiguration;
import de.enterprise.spring.boot.application.starter.actuate.servlet.ServletActuatorSecurityConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MeterRegistry.Config;

/**
 *
 * @author Malte Geßner
 *
 */
@Configuration(proxyBeanMethods = false)
@Import({ ServletActuatorSecurityConfiguration.class, ReactiveActuatorSecurityConfiguration.class })
@EnableConfigurationProperties(ActuatorProperties.class)
public class ActuatorAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnAvailableEndpoint
	public DocumentationMvcEndpoint documentationMvcEndpoint() {
		return new DocumentationMvcEndpoint();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 *
	 * @author Jonas Keßler
	 *
	 */
	@ConditionalOnClass(MeterRegistry.class)
	@Configuration
	public class MetricsConfig {

		@Bean
		MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(Environment environment, ActuatorProperties actuatorProperties) {
			return registry -> {
				Config config = registry.config().commonTags("profiles", StringUtils.join(environment.getActiveProfiles(), ","));

				if (StringUtils.isNotBlank(actuatorProperties.getInstanceCommonTagValue())) {
					config.commonTags("instance", actuatorProperties.getInstanceCommonTagValue());
				}

				if (StringUtils.isNotBlank(actuatorProperties.getVersionCommonTagValue())) {
					config.commonTags("version", actuatorProperties.getVersionCommonTagValue());
				}
			};
		}
	}
}
