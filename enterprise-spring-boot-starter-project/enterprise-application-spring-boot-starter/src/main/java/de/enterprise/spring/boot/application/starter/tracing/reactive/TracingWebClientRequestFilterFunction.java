package de.enterprise.spring.boot.application.starter.tracing.reactive;

import org.slf4j.MDC;
import org.springframework.boot.actuate.metrics.web.reactive.client.MetricsWebClientCustomizer;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import de.enterprise.spring.boot.application.starter.tracing.TracingProperties;
import de.enterprise.spring.boot.application.starter.tracing.TracingUtils;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * {@link ExchangeFilterFunction} applied via a {@link MetricsWebClientCustomizer} to record metrics.
 *
 * @author Brian Clozel
 * @author Tadaya Tsuyukubo
 * @since 2.1.0
 */
@RequiredArgsConstructor
public class TracingWebClientRequestFilterFunction implements ExchangeFilterFunction {

	private final TracingProperties tracingProperties;

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		request.headers().add(
				this.tracingProperties.getRequestHeaderName(),
				TracingUtils.retrieveOrCreate(
						MDC.get(this.tracingProperties.getMdcKey()),
						this.tracingProperties.getApplicationName()));

		return next.exchange(request);
	}

}
