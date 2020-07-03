package de.enterprise.spring.boot.application.starter.tracing.reactive;

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.web.reactive.function.client.WebClient;

import de.enterprise.spring.boot.application.starter.tracing.TracingProperties;

public class TracingWebClientCustomizer implements WebClientCustomizer {

	private final TracingWebClientRequestFilterFunction filterFunction;

	public TracingWebClientCustomizer(TracingProperties tracingProperties) {
		this.filterFunction = new TracingWebClientRequestFilterFunction(tracingProperties);
	}

	@Override
	public void customize(WebClient.Builder webClientBuilder) {
		webClientBuilder.filters((filterFunctions) -> {
			if (!filterFunctions.contains(this.filterFunction)) {
				filterFunctions.add(0, this.filterFunction);
			}
		});
	}
}
