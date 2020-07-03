package de.enterprise.spring.boot.application.starter.httpclient.reactive;

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.web.reactive.function.client.WebClient;

import de.enterprise.spring.boot.application.starter.logging.LoggingProperties;

public class RequestResponseWebClientCustomizer implements WebClientCustomizer {

	private final RequestResponseWebClientFilterFunction filterFunction;

	public RequestResponseWebClientCustomizer(LoggingProperties loggingProperties) {
		this.filterFunction = new RequestResponseWebClientFilterFunction(loggingProperties);
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
