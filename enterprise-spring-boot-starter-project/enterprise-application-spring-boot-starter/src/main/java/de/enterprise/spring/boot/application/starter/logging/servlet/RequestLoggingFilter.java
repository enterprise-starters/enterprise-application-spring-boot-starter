package de.enterprise.spring.boot.application.starter.logging.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.AbstractRequestLoggingFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Malte Geßner
 *
 */
@Slf4j(topic = "request-logger")
@ManagedResource(objectName = "de.enterprise.spring.boot.application:name=RequestLoggingFilter", description = "manage request logging filter")
public class RequestLoggingFilter extends AbstractRequestLoggingFilter {

	private String beforeMessagePrefix = DEFAULT_BEFORE_MESSAGE_PREFIX;
	private String beforeMessageSuffix = DEFAULT_BEFORE_MESSAGE_SUFFIX;
	private String afterMessagePrefix = DEFAULT_AFTER_MESSAGE_PREFIX;
	private String afterMessageSuffix = DEFAULT_AFTER_MESSAGE_SUFFIX;

	private boolean recordDuration;

	@ManagedOperation
	public boolean isRecordDuration() {
		return this.recordDuration;
	}

	@ManagedOperation
	public void setRecordDuration(boolean recordDuration) {
		this.recordDuration = recordDuration;
	}

	/**
	 * Forwards the request to the next filter in the chain and delegates down to the subclasses to perform the actual request logging both
	 * before and after the request is processed.
	 *
	 * @see #beforeRequest
	 * @see #afterRequest
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		boolean isFirstRequest = !this.isAsyncDispatch(request);
		HttpServletRequest requestToUse = request;

		if (this.isIncludePayload() && isFirstRequest && !(request instanceof ContentCachingRequestWrapper)) {
			requestToUse = new ContentCachingRequestWrapper(request, this.getMaxPayloadLength());
		}

		String requestMessage = "";

		boolean shouldLog = this.shouldLog(requestToUse);
		if (shouldLog && isFirstRequest) {
			requestMessage = this.getBeforeMessage(requestToUse);
		}

		StopWatch requestResponseTime = null;
		if (this.recordDuration) {
			requestResponseTime = new StopWatch();
		}

		try {
			if (this.recordDuration && requestResponseTime != null) {
				requestResponseTime.start();
			}
			filterChain.doFilter(requestToUse, response);
		} finally {
			String requestResponseDuration = "";
			if (this.recordDuration && requestResponseTime != null) {
				requestResponseTime.stop();
				requestResponseDuration = ", duration=" + requestResponseTime.getTotalTimeMillis();
			}
			if (shouldLog && !this.isAsyncStarted(requestToUse)) {
				this.afterRequest(requestToUse,
						requestMessage + ", " + this.getAfterMessage(requestToUse, response) + requestResponseDuration);
			}
		}
	}

	protected String getBeforeMessage(HttpServletRequest request) {
		return this.createRequestMessage(request, this.beforeMessagePrefix, this.beforeMessageSuffix);
	}

	protected String getAfterMessage(HttpServletRequest request, HttpServletResponse response) {
		return this.createResponseMessage(request, response, this.afterMessagePrefix, this.afterMessageSuffix);
	}

	/**
	 * Create a log message for the given request, prefix and suffix.
	 * <p>
	 * If {@code includeQueryString} is {@code true}, then the inner part of the log message will take the form
	 * {@code request_uri?query_string}; otherwise the message will simply be of the form {@code request_uri}.
	 * <p>
	 * The final message is composed of the inner part as described and the supplied prefix and suffix.
	 *
	 * @param request
	 *            request to log details for
	 * @param prefix
	 *            log message prefix
	 * @param suffix
	 *            log message suffix
	 * @return Log message as string
	 */
	protected String createRequestMessage(HttpServletRequest request, String prefix, String suffix) {
		StringBuilder msg = new StringBuilder();
		msg.append(prefix);
		msg.append("method=").append(request.getMethod());
		msg.append(";uri=").append(request.getRequestURI());

		if (this.isIncludeQueryString()) {
			String queryString = request.getQueryString();
			if (queryString != null) {
				msg.append('?').append(queryString);
			}
		}

		if (this.isIncludeClientInfo()) {
			String client = request.getRemoteAddr();
			if (StringUtils.hasLength(client)) {
				msg.append(";client=").append(client);
			}
			HttpSession session = request.getSession(false);
			if (session != null) {
				msg.append(";session=").append(session.getId());
			}
			String user = request.getRemoteUser();
			if (user != null) {
				msg.append(";user=").append(user);
			}
		}

		if (this.isIncludeHeaders()) {
			msg.append(";headers=").append(createHeaders(request));
		}

		msg.append(suffix);
		return msg.toString();
	}

	private String createHeaders(HttpServletRequest request) {
		List<String> resultList = new ArrayList<>();
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			Enumeration<String> headers = request.getHeaders(headerName);
			while (headers.hasMoreElements()) {
				resultList.add(headerName + ":\"" + headers.nextElement() + "\"");
			}
		}
		return resultList.toString();
	}

	protected String createResponseMessage(HttpServletRequest request, HttpServletResponse response, String prefix, String suffix) {
		StringBuilder msg = new StringBuilder();
		msg.append(prefix);
		msg.append("status=").append(response.getStatus());

		if (this.isIncludeHeaders()) {
			msg.append(", headers=").append(new ServletServerHttpResponse(response).getHeaders());
		}

		if (this.isIncludePayload()) {
			ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
			if (wrapper != null) {
				byte[] buf = wrapper.getContentAsByteArray();
				if (buf.length > 0) {
					int length = Math.min(buf.length, this.getMaxPayloadLength());
					String payload;
					try {
						payload = new String(buf, 0, length, wrapper.getCharacterEncoding());
					} catch (UnsupportedEncodingException ex) {
						payload = "[unknown]";
					}
					msg.append(";requestPayload=").append(payload);
				}
			}
		}

		msg.append(suffix);
		return msg.toString();
	}

	@Override
	protected boolean shouldLog(HttpServletRequest request) {
		return log.isInfoEnabled() && !request.getRequestURI().startsWith("/manage/");
	}

	/**
	 * Writes a log message before the request is processed.
	 */
	@Override
	protected void beforeRequest(HttpServletRequest request, String message) {
		log.info(message);
	}

	/**
	 * Writes a log message after the request is processed.
	 */
	@Override
	protected void afterRequest(HttpServletRequest request, String message) {
		log.info(message);
	}

	@Override
	public void setBeforeMessagePrefix(String beforeMessagePrefix) {
		this.beforeMessagePrefix = beforeMessagePrefix;
	}

	@Override
	public void setBeforeMessageSuffix(String beforeMessageSuffix) {
		this.beforeMessageSuffix = beforeMessageSuffix;
	}

	@Override
	public void setAfterMessagePrefix(String afterMessagePrefix) {
		this.afterMessagePrefix = afterMessagePrefix;
	}

	@Override
	public void setAfterMessageSuffix(String afterMessageSuffix) {
		this.afterMessageSuffix = afterMessageSuffix;
	}

	@ManagedOperation(description = "set include queryStrings")
	@ManagedOperationParameter(name = "includeQueryString", description = "enable/disable")
	@Override
	public void setIncludeQueryString(boolean includeQueryString) {
		super.setIncludeQueryString(includeQueryString);
	}

	@ManagedOperation(description = "is include queryStrings")
	@Override
	public boolean isIncludeQueryString() {
		return super.isIncludeQueryString();
	}

	@ManagedOperation(description = "set include clientInfo")
	@ManagedOperationParameter(name = "includeClientInfo", description = "enable/disable")
	@Override
	public void setIncludeClientInfo(boolean includeClientInfo) {
		super.setIncludeClientInfo(includeClientInfo);
	}

	@ManagedOperation(description = "is include clientInfo")
	@Override
	public boolean isIncludeClientInfo() {
		return super.isIncludeClientInfo();
	}

	@ManagedOperation(description = "set include headers")
	@ManagedOperationParameter(name = "includeHeaders", description = "enable/disable")
	@Override
	public void setIncludeHeaders(boolean includeHeaders) {
		super.setIncludeHeaders(includeHeaders);
	}

	@ManagedOperation(description = "is include headers")
	@Override
	public boolean isIncludeHeaders() {
		return super.isIncludeHeaders();
	}

	@ManagedOperation(description = "set include request payload")
	@ManagedOperationParameter(name = "includePayload", description = "enable/disable")
	@Override
	public void setIncludePayload(boolean includePayload) {
		super.setIncludePayload(includePayload);
	}

	@ManagedOperation(description = "is include request payload")
	@Override
	public boolean isIncludePayload() {
		return super.isIncludePayload();
	}

}
