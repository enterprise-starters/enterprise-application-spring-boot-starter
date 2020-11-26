package de.enterprise.spring.boot.application.starter.logging.reactive;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;

public class RequestLoggingFilterTest {

	private RequestLoggingFilter requestLoggingFilter;

	@BeforeEach
	public void setIp() {
		this.requestLoggingFilter = new RequestLoggingFilter();
	}

	@Test
	public void createResponseMessageWithHeader() {
		String sensitiveHeader = "sensitiveHeader";
		String headerKey = "someHeader";
		String headerValue = "someHeaderValue";
		this.requestLoggingFilter.setIncludeHeaders(true);
		this.requestLoggingFilter.setSensitiveHeaders(List.of(sensitiveHeader));

		MockServerHttpResponse response = new MockServerHttpResponse();
		response.getHeaders().add(headerKey, headerValue);

		String result = this.requestLoggingFilter.createResponseMessage(null, HttpStatus.OK, response, null, null);

		Assertions.assertThat(result).contains(String.format("%s:\"%s\"", headerKey, headerValue));
	}

	@Test
	public void createResponseMessageWithHeaderAndMaskSensitiveHeader() {
		String sensitiveHeader = "sensitiveHeader";
		String sensitiveHeaderValue = "sensitiveValue";
		this.requestLoggingFilter.setIncludeHeaders(true);
		this.requestLoggingFilter.setSensitiveHeaders(List.of(sensitiveHeader));

		MockServerHttpResponse response = new MockServerHttpResponse();
		response.getHeaders().add(sensitiveHeader, sensitiveHeaderValue);

		String result = this.requestLoggingFilter.createResponseMessage(null, HttpStatus.OK, response, null, null);

		Assertions.assertThat(result).contains(String.format("%s:\"**********\"", sensitiveHeader));
		Assertions.assertThat(result).doesNotContain(sensitiveHeaderValue);
	}

	@Test
	public void createRequestMessageWithHeader() {
		String sensitiveHeader = "sensitiveHeader";
		String headerKey = "someHeader";
		String headerValue = "someHeaderValue";
		this.requestLoggingFilter.setIncludeHeaders(true);
		this.requestLoggingFilter.setSensitiveHeaders(List.of(sensitiveHeader));

		MockServerHttpRequest request = MockServerHttpRequest.get("test")
				.header(headerKey, headerValue)
				.build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		String result = this.requestLoggingFilter.createRequestMessage(exchange, null, null);

		Assertions.assertThat(result).contains(String.format("%s:\"%s\"", headerKey, headerValue));
	}

	@Test
	public void createRequestMessageWithHeaderAndMaskSensitiveHeader() {
		String sensitiveHeader = "sensitiveHeader";
		String sensitiveHeaderValue = "sensitiveValue";
		this.requestLoggingFilter.setIncludeHeaders(true);
		this.requestLoggingFilter.setSensitiveHeaders(List.of(sensitiveHeader));

		MockServerHttpRequest request = MockServerHttpRequest.get("test")
				.header(sensitiveHeader, sensitiveHeaderValue)
				.build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		String result = this.requestLoggingFilter.createRequestMessage(exchange, null, null);

		Assertions.assertThat(result).contains(String.format("%s:\"**********\"", sensitiveHeader));
		Assertions.assertThat(result).doesNotContain(sensitiveHeaderValue);

	}
}
