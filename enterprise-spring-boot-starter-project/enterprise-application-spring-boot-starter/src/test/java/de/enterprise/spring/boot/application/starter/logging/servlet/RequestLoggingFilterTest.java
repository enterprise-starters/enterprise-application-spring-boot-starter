package de.enterprise.spring.boot.application.starter.logging.servlet;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class RequestLoggingFilterTest {

	private RequestLoggingFilter requestLoggingFilter;

	@BeforeEach
	public void setUp() {
		this.requestLoggingFilter = new RequestLoggingFilter();
	}

	@Test
	public void createRequestMessageWithHeaderTest() {
		String sensitiveHeader = "sensitiveHeader";
		String headerKey = "someHeader";
		String headerValue = "someHeaderValue";
		this.requestLoggingFilter.setIncludeHeaders(true);
		this.requestLoggingFilter.setSensitiveHeaders(List.of(sensitiveHeader));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(headerKey, headerValue);

		String result = this.requestLoggingFilter.createRequestMessage(request, null, null);

		Assertions.assertThat(result).contains(String.format("%s:\"%s\"", headerKey, headerValue));
	}

	@Test
	public void createRequestMessageWithHeaderTestAndMaskSensitiveHeader() {
		String sensitiveHeader = "sensitiveHeader";
		String sensitiveHeaderValue = "sensitiveValue";
		this.requestLoggingFilter.setIncludeHeaders(true);
		this.requestLoggingFilter.setSensitiveHeaders(List.of(sensitiveHeader));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(sensitiveHeader, sensitiveHeaderValue);

		String result = this.requestLoggingFilter.createRequestMessage(request, null, null);

		Assertions.assertThat(result).contains(String.format("%s:\"**********\"", sensitiveHeader));
		Assertions.assertThat(result).doesNotContain(sensitiveHeaderValue);
	}
}
