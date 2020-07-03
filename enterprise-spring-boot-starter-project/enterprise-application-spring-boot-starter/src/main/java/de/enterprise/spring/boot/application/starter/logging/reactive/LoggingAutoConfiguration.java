package de.enterprise.spring.boot.application.starter.logging.reactive;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.enterprise.spring.boot.application.starter.logging.LoggingProperties;

/**
 * Configuration class for to auto configure the log web request handling.
 *
 * @author Malte Geßner
 *
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.REACTIVE)
@EnableConfigurationProperties(LoggingProperties.class)
public class LoggingAutoConfiguration {

	/**
	 * Configuration for enterprise-application.logging properties.
	 *
	 * @author Malte Geßner
	 *
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "enterprise-application.logging", name = "web-enabled", havingValue = "true", matchIfMissing = true)
	public static class WebLoggingEnabled {

		@Bean
		public RequestLoggingFilter requestLoggingFilter(LoggingProperties loggingProperties) {
			RequestLoggingFilter requestLoggingFilter = new RequestLoggingFilter();
			requestLoggingFilter.setBeforeMessagePrefix("request ");
			requestLoggingFilter.setBeforeMessageSuffix("");
			requestLoggingFilter.setAfterMessagePrefix("response ");
			requestLoggingFilter.setAfterMessageSuffix("");
			requestLoggingFilter.setIncludeQueryString(loggingProperties.isLogIncomingRequestWithQueryString());
			requestLoggingFilter.setIncludePayload(loggingProperties.isLogIncomingRequestWithPayload());
			requestLoggingFilter.setMaxPayloadLength(loggingProperties.getLogIncomingRequestMaxPayloadLength());
			requestLoggingFilter.setIncludeHeaders(loggingProperties.isLogIncomingRequestWithHeaders());
			requestLoggingFilter.setIncludeClientInfo(loggingProperties.isLogIncomingRequestWithClientInfo());

			return requestLoggingFilter;
		}
	}
}
