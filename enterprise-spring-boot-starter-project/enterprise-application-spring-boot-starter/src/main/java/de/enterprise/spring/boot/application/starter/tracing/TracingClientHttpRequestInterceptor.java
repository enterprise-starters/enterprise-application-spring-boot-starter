package de.enterprise.spring.boot.application.starter.tracing;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * {@link ClientHttpRequestInterceptor} applied via a {@link TracingRestTemplateCustomizer} to add tracing header to calls.
 *
 * @author Malte Geßner
 */
class TracingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

	private TracingProperties tracingProperties;

	TracingClientHttpRequestInterceptor(TracingProperties tracingProperties) {
		this.tracingProperties = tracingProperties;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		request.getHeaders().add(
				this.tracingProperties.getRequestHeaderName(),
				TracingUtils.retrieveOrCreate(
						MDC.get(this.tracingProperties.getMdcKey()),
						this.tracingProperties.getApplicationName()));

		return execution.execute(request, body);
	}
}