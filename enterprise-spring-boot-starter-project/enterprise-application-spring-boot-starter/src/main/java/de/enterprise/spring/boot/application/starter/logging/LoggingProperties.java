package de.enterprise.spring.boot.application.starter.logging;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

/**
 * Central properties class for logging config.
 *
 * @author Malte Geßner
 *
 */

@ConfigurationProperties(prefix = "enterprise-application.logging")
@Getter
@Setter
@Validated
public class LoggingProperties {

	@NotNull
	private String filePrefix = "";
	@NotNull
	private String appName;
	@NotNull
	private String appenderContainerConsolePattern;
	@NotNull
	private String appenderLogfilePattern;
	@NotNull
	private String appenderConsolePattern;

	/*
	 * Incoming requests (Controller)
	 */
	private boolean webEnabled = true;
	private boolean logIncomingRequestWithQueryString = true;
	private boolean logIncomingRequestWithPayload;
	private int logIncomingRequestMaxPayloadLength;
	private boolean logIncomingRequestWithHeaders;
	private boolean logIncomingRequestWithClientInfo;
	private int filterOrder = OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER - 102;

	/*
	 * Outgoing Requests (RestTemplate)
	 */
	private boolean logOutgoingRequestDetailsEnabled = false;
	private List<String> sensitiveRequestParameters;
	private List<String> sensitiveOutgoingHeaders;

	// TODO: Struktur einführen, um Properties für RestTemplate und Controller besser unterscheiden zu können
}
