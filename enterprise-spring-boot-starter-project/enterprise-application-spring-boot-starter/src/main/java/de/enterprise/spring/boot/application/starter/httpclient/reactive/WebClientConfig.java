package de.enterprise.spring.boot.application.starter.httpclient.reactive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.WebClient;

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
public class WebClientConfig extends de.enterprise.spring.boot.application.starter.httpclient.HttpClientConfig {

	@Autowired(required = false)
	@Setter(AccessLevel.PRIVATE)
	private WebClient.Builder webClientBuilder;

	public WebClient.Builder createPreConfiguredWebClientBuilder() {
		WebClient.Builder clonedBuilder = this.webClientBuilder;

		return clonedBuilder.baseUrl(this.getBaseAddress());
	}

}
