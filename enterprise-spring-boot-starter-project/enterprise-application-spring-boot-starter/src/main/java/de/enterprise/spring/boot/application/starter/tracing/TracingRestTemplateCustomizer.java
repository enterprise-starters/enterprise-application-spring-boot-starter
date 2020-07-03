package de.enterprise.spring.boot.application.starter.tracing;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import de.enterprise.spring.boot.application.starter.tracing.servlet.TracingClientHttpRequestInterceptor;

public class TracingRestTemplateCustomizer implements RestTemplateCustomizer {

	private ClientHttpRequestInterceptor interceptor;

	public TracingRestTemplateCustomizer(TracingProperties tracingProperties) {
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
