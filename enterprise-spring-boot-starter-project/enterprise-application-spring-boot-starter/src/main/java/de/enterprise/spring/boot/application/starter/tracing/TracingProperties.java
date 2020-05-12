package de.enterprise.spring.boot.application.starter.tracing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

/**
 * All properties to configure the tracing token features.
 *
 * @author Malte Geßner
 *
 */
@ConfigurationProperties(prefix = "enterprise-application.tracing")
@Validated
@Getter
@Setter
public class TracingProperties {
	private boolean enabled = true;
	private String requestHeaderName = "X-TraceId";
	private String mdcKey = "traceId";
	private String applicationName;
	private int filterOrder = OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER - 103;
}
