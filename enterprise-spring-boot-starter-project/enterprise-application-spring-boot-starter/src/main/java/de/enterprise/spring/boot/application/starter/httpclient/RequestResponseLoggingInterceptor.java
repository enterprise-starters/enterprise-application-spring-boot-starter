package de.enterprise.spring.boot.application.starter.httpclient;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Logging interceptor for RestTemplates. Inspired by Scott Bock, see link below.
 *
 * @author Jonas Keßler
 *
 * @see <a href="https://objectpartners.com/2018/03/01/log-your-resttemplate-request-and-response-without-destroying-the-body/">link</a>
 */
@Slf4j
public class RequestResponseLoggingInterceptor implements ClientHttpRequestInterceptor {

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		String requestUuid = UUID.randomUUID().toString();
		String requestDetails = logRequest(requestUuid, request, body);
		ClientHttpResponse response = execution.execute(request, body);
		logResponse(requestUuid, requestDetails, response);
		return response;
	}

	private String logRequest(String requestUuid, HttpRequest request, byte[] body) throws IOException {
		String requestdetails = "";
		if (log.isInfoEnabled()) {
			StringBuilder requestDetailsBuilder = new StringBuilder();
			requestDetailsBuilder.append("method=").append(request.getMethod()).append(";")
					.append("uri=").append(request.getURI());
			requestdetails = requestDetailsBuilder.toString();

			StringBuilder msg = new StringBuilder();
			msg.append("Outgoing REST request with requestUuid=").append(requestUuid).append("\r\n")
					.append(requestdetails).append("\r\n")
					.append("headers=").append(request.getHeaders());
			if (body.length > 0) {
				msg.append("\r\n").append("requestBody=").append(new String(body, "UTF-8"));
			}
			log.info(msg.toString());
		}
		return requestdetails;
	}

	private void logResponse(String requestUuid, String requestDetails, ClientHttpResponse response) throws IOException {
		if (log.isInfoEnabled()) {
			StringBuilder msg = new StringBuilder();
			msg.append("Incoming REST response with requestUuid=").append(requestUuid).append("\r\n")
					.append(requestDetails).append("\r\n")
					.append("statusCode=").append(response.getStatusCode()).append(";")
					.append("statusText=").append(response.getStatusText()).append("\r\n")
					.append("headers=").append(response.getHeaders());

			String responseBody = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
			if (!StringUtils.isBlank(responseBody)) {
				msg.append("\r\n").append("responseBody=").append(StreamUtils.copyToString(response.getBody(), Charset.defaultCharset()));
			}
			log.info(msg.toString());
		}
	}
}
