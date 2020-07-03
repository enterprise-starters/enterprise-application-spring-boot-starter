package de.enterprise.spring.boot.application.starter.httpclient.reactive;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import de.enterprise.spring.boot.application.starter.logging.LoggingProperties;

/**
 * Configuration class for to auto configure the webclient request/response logging handling.
 *
 * @author Malte Geßner
 *
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.REACTIVE)
@ConditionalOnClass(WebClient.class)
public class WebClientRequestResponseLoggingAutoConfiguration {

	@Bean
	RequestResponseWebClientCustomizer requestResponseWebClientCustomizer(LoggingProperties loggingProperties) {
		return new RequestResponseWebClientCustomizer(loggingProperties);
	}

}
