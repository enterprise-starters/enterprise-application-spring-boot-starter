package de.enterprise.spring.boot.application.starter.tracing;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration class for to auto configure the tracing handling e.g. in commons http client.
 *
 * @author Malte Geßner
 *
 */
@Configuration
@EnableConfigurationProperties(TracingProperties.class)
@ConditionalOnProperty(prefix = "enterprise-application.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class TracingAutoConfiguration {

	@Bean
	public FilterRegistrationBean<TracingHeaderFilter> tracingHeaderFilter(TracingProperties tracingProperties) {
		log.debug("tracing header filter enabled, using requestHeaderName:{}, mdcKey:{}", tracingProperties.getRequestHeaderName(),
				tracingProperties.getMdcKey());

		TracingHeaderFilter filter = new TracingHeaderFilter(tracingProperties);

		FilterRegistrationBean<TracingHeaderFilter> filterRegistrationBean = new FilterRegistrationBean<>();
		filterRegistrationBean.setFilter(filter);
		filterRegistrationBean.setOrder(tracingProperties.getFilterOrder());

		return filterRegistrationBean;

	}

	@Bean
	TracingRestTemplateCustomizer tracingRestTemplateCustomizer(TracingProperties tracingProperties) {
		return new TracingRestTemplateCustomizer(tracingProperties);
	}

	private static class TracingRestTemplateCustomizer implements RestTemplateCustomizer {

		private ClientHttpRequestInterceptor interceptor;

		TracingRestTemplateCustomizer(TracingProperties tracingProperties) {
			this.interceptor = new TracingClientHttpRequestInterceptor(tracingProperties);
		}

		@Override
		public void customize(RestTemplate restTemplate) {
			List<ClientHttpRequestInterceptor> existingInterceptors = restTemplate
					.getInterceptors();
			if (!existingInterceptors.contains(this.interceptor)) {
				List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
				interceptors.add(this.interceptor);
				interceptors.addAll(existingInterceptors);
				restTemplate.setInterceptors(interceptors);
			}
		}
	}
}
