package de.enterprise.spring.boot.application.starter.tracing.reactive;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebSession;

import de.enterprise.spring.boot.application.starter.tracing.TracingProperties;
import de.enterprise.spring.boot.application.starter.tracing.TracingUtils;
import reactor.core.publisher.Mono;

public class TracingHeaderWebFilter implements WebFilter, Ordered {
	private TracingProperties tracingProperties;

	public TracingHeaderWebFilter(TracingProperties tracingProperties) {
		this.tracingProperties = tracingProperties;
	}

	private int order = Ordered.LOWEST_PRECEDENCE - 12;

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		this.addSessionId(exchange.getSession());
		TracingUtils.addMdcTraceContext(this.tracingProperties,
				exchange.getRequest().getHeaders().getFirst(this.tracingProperties.getRequestHeaderName()));

		String mdcKey = this.tracingProperties.getMdcKey();
		String sessionIdKey = this.tracingProperties.getSessionIdKey();

		return chain.filter(exchange).doOnSuccess(t -> {
			MDC.remove(sessionIdKey);
			MDC.remove(mdcKey);
		}).doOnError(ex -> {
			MDC.remove(sessionIdKey);
			MDC.remove(mdcKey);
		});
	}

	private void addSessionId(Mono<WebSession> session) {
		session.doOnNext(webSession -> MDC.put(this.tracingProperties.getSessionIdKey(), webSession.getId()));
	}

}
