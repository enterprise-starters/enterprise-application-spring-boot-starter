package de.enterprise.spring.boot.application.starter.logging.reactive;

import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j(topic = "request-logger")
@ManagedResource(objectName = "de.enterprise.spring.boot.application:name=RequestLoggingFilter", description = "manage request logging filter")
public class RequestLoggingFilter implements WebFilter, Ordered {

	private boolean recordDuration;

	/**
	 * The default value prepended to the log message written <i>before</i> a request is processed.
	 */
	public static final String DEFAULT_BEFORE_MESSAGE_PREFIX = "Before request [";

	/**
	 * The default value appended to the log message written <i>before</i> a request is processed.
	 */
	public static final String DEFAULT_BEFORE_MESSAGE_SUFFIX = "]";

	/**
	 * The default value prepended to the log message written <i>after</i> a request is processed.
	 */
	public static final String DEFAULT_AFTER_MESSAGE_PREFIX = "After request [";

	/**
	 * The default value appended to the log message written <i>after</i> a request is processed.
	 */
	public static final String DEFAULT_AFTER_MESSAGE_SUFFIX = "]";

	private static final int DEFAULT_MAX_PAYLOAD_LENGTH = 50;

	private boolean includeQueryString = false;

	private boolean includeClientInfo = false;

	private boolean includeHeaders = false;

	private boolean includePayload = false;

	@Nullable
	private Predicate<String> headerPredicate;

	private int maxPayloadLength = DEFAULT_MAX_PAYLOAD_LENGTH;

	private String beforeMessagePrefix = DEFAULT_BEFORE_MESSAGE_PREFIX;

	private String beforeMessageSuffix = DEFAULT_BEFORE_MESSAGE_SUFFIX;

	private String afterMessagePrefix = DEFAULT_AFTER_MESSAGE_PREFIX;

	private String afterMessageSuffix = DEFAULT_AFTER_MESSAGE_SUFFIX;
	private int order = Ordered.LOWEST_PRECEDENCE - 11;

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Forwards the request to the next filter in the chain and delegates down to the subclasses to perform the actual request logging both
	 * before and after the request is processed.
	 */
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

		ServerHttpRequest requestToUse = exchange.getRequest();

		// if (this.isIncludePayload() && !(request instanceof ContentCachingRequestWrapper)) {
		// requestToUse = new ContentCachingRequestWrapper(request, this.getMaxPayloadLength());
		// }

		boolean shouldLog = this.shouldLog(requestToUse);
		final String requestMessage = shouldLog ? getBeforeMessage(exchange) : "";

		final StopWatch requestResponseTime = this.recordDuration ? new StopWatch() : null;

		if (requestResponseTime != null) {
			requestResponseTime.start();
		}

		return chain.filter(exchange).doOnSuccess(t -> {
			String requestResponseDuration = "";
			if (requestResponseTime != null) {
				requestResponseTime.stop();
				requestResponseDuration = ", duration=" + requestResponseTime.getTotalTimeMillis();
			}
			if (shouldLog) {
				afterRequest(requestMessage + ", "
						+ getAfterMessage(requestToUse, exchange.getResponse().getStatusCode(), exchange.getResponse())
						+ requestResponseDuration);
			}
		}).doOnError(ex -> {
			String requestResponseDuration = "";
			if (requestResponseTime != null) {
				requestResponseTime.stop();
				requestResponseDuration = ", duration=" + requestResponseTime.getTotalTimeMillis();
			}
			if (shouldLog) {
				HttpStatus responseStatus = exchange.getResponse().getStatusCode();
				if (ex instanceof ResponseStatusException) {
					responseStatus = ((ResponseStatusException) ex).getStatus();
				}

				afterRequest(requestMessage + ", "
						+ getAfterMessage(requestToUse, responseStatus, exchange.getResponse())
						+ requestResponseDuration);
			}
		});

	}

	protected void afterRequest(String message) {
		log.info(message);
	}

	protected String getBeforeMessage(ServerWebExchange exchange) {
		return this.createRequestMessage(exchange, this.beforeMessagePrefix, this.beforeMessageSuffix);
	}

	protected String getAfterMessage(ServerHttpRequest request, HttpStatus responseStatus, ServerHttpResponse response) {
		return this.createResponseMessage(request, responseStatus, response, this.afterMessagePrefix, this.afterMessageSuffix);
	}

	protected String createResponseMessage(ServerHttpRequest request, HttpStatus responseStatus, ServerHttpResponse response, String prefix,
			String suffix) {
		StringBuilder msg = new StringBuilder();
		msg.append(prefix);
		msg.append("status=").append(responseStatus.value());

		if (this.isIncludeHeaders()) {
			// TODO mask sensitive headers
			msg.append(", headers=").append(response.getHeaders());
		}

		// if (this.isIncludePayload()) {
		// ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
		// if (wrapper != null) {
		// byte[] buf = wrapper.getContentAsByteArray();
		// if (buf.length > 0) {
		// int length = Math.min(buf.length, this.getMaxPayloadLength());
		// String payload;
		// try {
		// payload = new String(buf, 0, length, wrapper.getCharacterEncoding());
		// } catch (UnsupportedEncodingException ex) {
		// payload = "[unknown]";
		// }
		// msg.append(";requestPayload=").append(payload);
		// }
		// }
		// }

		msg.append(suffix);
		return msg.toString();
	}

	/**
	 * Create a log message for the given request, prefix and suffix.
	 * <p>
	 * If {@code includeQueryString} is {@code true}, then the inner part of the log message will take the form
	 * {@code request_uri?query_string}; otherwise the message will simply be of the form {@code request_uri}.
	 * <p>
	 * The final message is composed of the inner part as described and the supplied prefix and suffix.
	 *
	 * @param exchange
	 *            exchange with request to log details
	 * @param prefix
	 *            log message prefix
	 * @param suffix
	 *            log message suffix
	 * @return Log message as string
	 */
	protected String createRequestMessage(ServerWebExchange exchange, String prefix, String suffix) {
		StringBuilder msg = new StringBuilder();
		msg.append(prefix);
		msg.append("method=").append(exchange.getRequest().getMethod());
		msg.append(";uri=").append(exchange.getRequest().getPath());

		if (this.isIncludeQueryString() && !exchange.getRequest().getQueryParams().isEmpty()) {
			String queryString = exchange.getRequest().getQueryParams().entrySet().stream()
					.map(entry -> entry.getKey().concat("=").concat(entry.getValue().get(0))).collect(Collectors.joining("&"));
			if (queryString != null) {
				msg.append('?').append(queryString);
			}
		}

		if (this.isIncludeClientInfo()) {
			String client = exchange.getRequest().getRemoteAddress().getHostString();
			if (StringUtils.hasLength(client)) {
				msg.append(";client=").append(client);
			}
			exchange.getSession().doOnNext(webSession -> {
				msg.append(";session=").append(webSession.getId());
			});

			exchange.getPrincipal().doOnNext(principal -> {
				msg.append(";user=").append(principal);
			});
		}

		if (this.isIncludeHeaders()) {
			msg.append(";headers=").append(exchange.getRequest().getHeaders());
		}

		msg.append(suffix);
		return msg.toString();
	}

	@ManagedOperation
	public boolean isRecordDuration() {
		return this.recordDuration;
	}

	@ManagedOperation
	public void setRecordDuration(boolean recordDuration) {
		this.recordDuration = recordDuration;
	}

	@ManagedOperation(description = "set include queryStrings")
	@ManagedOperationParameter(name = "includeQueryString", description = "enable/disable")
	public void setIncludeQueryString(boolean includeQueryString) {
		this.includeQueryString = includeQueryString;
	}

	@ManagedOperation(description = "is include queryStrings")
	protected boolean isIncludeQueryString() {
		return this.includeQueryString;
	}

	@ManagedOperation(description = "set include clientInfo")
	@ManagedOperationParameter(name = "includeClientInfo", description = "enable/disable")
	public void setIncludeClientInfo(boolean includeClientInfo) {
		this.includeClientInfo = includeClientInfo;
	}

	@ManagedOperation(description = "is include clientInfo")
	protected boolean isIncludeClientInfo() {
		return this.includeClientInfo;
	}

	@ManagedOperation(description = "set include headers")
	@ManagedOperationParameter(name = "includeHeaders", description = "enable/disable")
	public void setIncludeHeaders(boolean includeHeaders) {
		this.includeHeaders = includeHeaders;
	}

	@ManagedOperation(description = "is include headers")
	protected boolean isIncludeHeaders() {
		return this.includeHeaders;
	}

	@ManagedOperation(description = "set include request payload")
	@ManagedOperationParameter(name = "includePayload", description = "enable/disable")
	public void setIncludePayload(boolean includePayload) {
		this.includePayload = includePayload;
	}

	@ManagedOperation(description = "is include request payload")
	protected boolean isIncludePayload() {
		return this.includePayload;
	}

	public void setHeaderPredicate(@Nullable Predicate<String> headerPredicate) {
		this.headerPredicate = headerPredicate;
	}

	@Nullable
	protected Predicate<String> getHeaderPredicate() {
		return this.headerPredicate;
	}

	public void setMaxPayloadLength(int maxPayloadLength) {
		Assert.isTrue(maxPayloadLength >= 0, "'maxPayloadLength' should be larger than or equal to 0");
		this.maxPayloadLength = maxPayloadLength;
	}

	protected int getMaxPayloadLength() {
		return this.maxPayloadLength;
	}

	public void setBeforeMessagePrefix(String beforeMessagePrefix) {
		this.beforeMessagePrefix = beforeMessagePrefix;
	}

	public void setBeforeMessageSuffix(String beforeMessageSuffix) {
		this.beforeMessageSuffix = beforeMessageSuffix;
	}

	public void setAfterMessagePrefix(String afterMessagePrefix) {
		this.afterMessagePrefix = afterMessagePrefix;
	}

	public void setAfterMessageSuffix(String afterMessageSuffix) {
		this.afterMessageSuffix = afterMessageSuffix;
	}

	protected boolean shouldLog(ServerHttpRequest request) {
		return true;
	}
}
