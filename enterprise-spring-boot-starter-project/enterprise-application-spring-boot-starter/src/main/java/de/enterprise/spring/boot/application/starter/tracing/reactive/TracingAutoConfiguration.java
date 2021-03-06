package de.enterprise.spring.boot.application.starter.tracing.reactive;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.enterprise.spring.boot.application.starter.tracing.TracingProperties;
import de.enterprise.spring.boot.application.starter.tracing.TracingRestTemplateCustomizer;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration class for to auto configure the tracing handling e.g. in commons http client.
 *
 * @author Malte Geßner
 *
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.REACTIVE)
@EnableConfigurationProperties(TracingProperties.class)
@ConditionalOnProperty(prefix = "enterprise-application.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class TracingAutoConfiguration {

	@Bean
	TracingRestTemplateCustomizer tracingRestTemplateCustomizer(TracingProperties tracingProperties) {
		return new TracingRestTemplateCustomizer(tracingProperties);
	}

	@Bean
	public TracingHeaderWebFilter tracingHeaderFilter(TracingProperties tracingProperties) {
		log.debug("tracing header filter enabled, using requestHeaderName:{}, mdcKey:{}", tracingProperties.getRequestHeaderName(),
				tracingProperties.getMdcKey());

		return new TracingHeaderWebFilter(tracingProperties);
	}

}
