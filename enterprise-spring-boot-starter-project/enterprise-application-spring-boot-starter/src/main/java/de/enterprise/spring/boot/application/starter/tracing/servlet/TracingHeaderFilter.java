package de.enterprise.spring.boot.application.starter.tracing.servlet;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import de.enterprise.spring.boot.application.starter.tracing.TracingProperties;
import de.enterprise.spring.boot.application.starter.tracing.TracingUtils;

/**
 * Servlet Filter implementation to add tracing info to {@link MDC} thread local context.
 *
 * @author Malte Geßner
 *
 */
public class TracingHeaderFilter extends OncePerRequestFilter {

	private TracingProperties tracingProperties;
	private String sessionIdKey;

	public TracingHeaderFilter(TracingProperties tracingProperties) {
		this.tracingProperties = tracingProperties;
		this.sessionIdKey = "sessionId";
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String sessionId = this.retrieveSessionId(request);
		MDC.put(this.sessionIdKey, sessionId);
		TracingUtils.addMdcTraceContext(this.tracingProperties,
				request.getHeader(this.tracingProperties.getRequestHeaderName()));
		filterChain.doFilter(request, response);
		TracingUtils.removeMdcTraceContext(this.tracingProperties);
		MDC.remove(this.sessionIdKey);
	}

	private String retrieveSessionId(HttpServletRequest request) {
		HttpSession session = request.getSession();
		if (session != null) {
			return session.getId();
		}
		return null;
	}

	@Override
	public void destroy() {
		// nothing to do here
	}
}
