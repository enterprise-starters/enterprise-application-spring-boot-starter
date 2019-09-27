package de.enterprise.spring.boot.application.starter.httpclient;

import java.time.Duration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.enterprise.spring.boot.application.starter.logging.LoggingProperties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration parameters for http client configuration.
 *
 * @author Malte Geßner
 *
 */
@Getter
@Setter
@Validated
public class HttpClientConfig {

	@Autowired
	@Setter(AccessLevel.PRIVATE)
	private RestTemplateBuilder restTemplateBuilder;

	@Autowired
	@Setter(AccessLevel.PRIVATE)
	private ObjectMapper applicationConfiguredDefaultJacksonObjectMapper;

	@Autowired
	@Setter(AccessLevel.PRIVATE)
	private LoggingProperties loggingProperties;

	/**
	 * base address.
	 */
	@NotNull
	private String baseAddress;

	/**
	 * Set the timeout in milliseconds used when requesting a connection from the connection. A timeout value of 0 specifies an infinite
	 * timeout.
	 */
	@Min(1)
	private int connectionRequestTimeout = 5 * 1000;
	/**
	 * Set the connection timeout in milliseconds. A timeout value of 0 specifies an infinite timeout.
	 */
	@Min(1)
	private int connectTimeout = 5 * 1000;
	/**
	 * Set the socket read timeout in milliseconds. A timeout value of 0 specifies an infinite timeout.
	 */
	@Min(1)
	private int readTimeout = 30 * 1000;

	/**
	 * Whether to log details of each request and response. Specific property for this restTemplate instance which overwrites the general
	 * setting ({@link LoggingProperties#isLogOutgoingRequestDetailsEnabled()}.
	 */
	private Boolean logDetailsEnabled;

	@Valid
	private BasicAuth basicAuth;

	/**
	 * Creates a new configured restTemplateBuilder instance with values from this config class. Given customizedMessageConverters replaced
	 * all existing message converters.
	 *
	 * @param customizedMessageConverters
	 *            Optional list of message converters to apply to the restTemplate
	 * @return Configured RestTemplateBuilder instance
	 */
	public RestTemplateBuilder createPreConfiguredRestTemplateBuilder(HttpMessageConverter<?>... customizedMessageConverters) {
		if (this.getBasicAuth() != null) {
			this.restTemplateBuilder = this.restTemplateBuilder.basicAuthentication(this.getBasicAuth().getUsername(),
					this.getBasicAuth().getPassword());
		}

		if (this.logDetailsEnabled != null && this.logDetailsEnabled
				|| this.logDetailsEnabled == null && this.loggingProperties.isLogOutgoingRequestDetailsEnabled()) {
			this.restTemplateBuilder = this.restTemplateBuilder
					.requestFactory(() -> new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
					.additionalInterceptors(new RequestResponseLoggingInterceptor());
		}

		if (customizedMessageConverters.length == 0) {
			this.restTemplateBuilder = this.restTemplateBuilder.additionalCustomizers(restTemplate -> {
				Set<Class<?>> uniqueMessageConverterTypes = new HashSet<>();

				Iterator<HttpMessageConverter<?>> httpMessageConverterItr = restTemplate.getMessageConverters().iterator();
				while (httpMessageConverterItr.hasNext()) {
					HttpMessageConverter<?> httpMessageConverter = httpMessageConverterItr.next();
					if (!uniqueMessageConverterTypes.contains(httpMessageConverter.getClass())) {
						if (httpMessageConverter instanceof MappingJackson2HttpMessageConverter) {
							((MappingJackson2HttpMessageConverter) httpMessageConverter)
									.setObjectMapper(this.applicationConfiguredDefaultJacksonObjectMapper);
						}
						uniqueMessageConverterTypes.add(httpMessageConverter.getClass());
					} else {
						httpMessageConverterItr.remove();
					}
				}
			});
		} else {
			this.restTemplateBuilder = this.restTemplateBuilder.messageConverters(customizedMessageConverters);
		}

		return this.restTemplateBuilder.rootUri(this.getBaseAddress())
				.setConnectTimeout(Duration.ofMillis(this.getConnectTimeout()))
				.setReadTimeout(Duration.ofMillis(this.getReadTimeout()));
	}

	/**
	 *
	 * @author Jonas Keßler
	 */
	@Getter
	@Setter
	public static class BasicAuth {
		@NotNull
		private String username;
		@NotNull
		private String password;
	}

}
