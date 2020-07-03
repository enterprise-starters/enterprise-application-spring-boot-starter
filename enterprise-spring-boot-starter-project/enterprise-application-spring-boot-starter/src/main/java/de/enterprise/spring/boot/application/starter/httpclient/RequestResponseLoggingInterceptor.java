package de.enterprise.spring.boot.application.starter.httpclient;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import de.enterprise.spring.boot.application.starter.logging.LoggingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Logging interceptor for RestTemplates. Inspired by Scott Bock, see link below.
 *
 * @author Jonas Keßler
 *
 * @see <a href="https://objectpartners.com/2018/03/01/log-your-resttemplate-request-and-response-without-destroying-the-body/">link</a>
 */
@Slf4j(topic = "request-logger-outbound")
@RequiredArgsConstructor
public class RequestResponseLoggingInterceptor implements ClientHttpRequestInterceptor {

	private static final String VALUE_SEPARATOR = "; ";

	private final LoggingProperties loggingProperties;

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
			requestDetailsBuilder.append("method=").append(request.getMethod()).append(VALUE_SEPARATOR)
					.append("uri=").append(maskSensitiveParamters(request.getURI()));
			requestdetails = requestDetailsBuilder.toString();

			StringBuilder msg = new StringBuilder();
			msg.append("Outgoing REST request with requestUuid=").append(requestUuid).append(VALUE_SEPARATOR)
					.append(requestdetails).append(VALUE_SEPARATOR)
					.append("headers=").append(maskSensitiveHeaders(request.getHeaders()));
			if (this.loggingProperties.isLogOutgoingRequestWithPayload()) {
				boolean multipartBody = request.getHeaders().getContentType() != null
						&& "multipart".equals(request.getHeaders().getContentType().getType());
				if (!multipartBody && body.length > 0) {
					msg.append(VALUE_SEPARATOR).append("requestBody=").append(new String(body, "UTF-8"));
				}
			}
			log.info(msg.toString());
		}
		return requestdetails;
	}

	private void logResponse(String requestUuid, String requestDetails, ClientHttpResponse response) throws IOException {
		if (log.isInfoEnabled()) {
			StringBuilder msg = new StringBuilder();
			msg.append("Incoming REST response with requestUuid=").append(requestUuid).append(VALUE_SEPARATOR)
					.append(requestDetails).append(VALUE_SEPARATOR)
					.append("statusCode=").append(response.getStatusCode()).append(VALUE_SEPARATOR)
					.append("statusText=").append(response.getStatusText()).append(VALUE_SEPARATOR)
					.append("headers=").append(response.getHeaders());
			if (this.loggingProperties.isLogOutgoingRequestWithPayload()) {
				String responseBody = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
				if (!StringUtils.isBlank(responseBody)) {
					msg.append(VALUE_SEPARATOR).append("responseBody=").append(responseBody);
				}
			}
			log.info(msg.toString());
		}
	}

	/**
	 * Creates a copy of the incoming headers object, replacing all sensitive headers with stars. If list of sensitive header keys is empty,
	 * the original object is returned.
	 *
	 * @param headers
	 *            Incoming header, can contain sensitive values.
	 * @return Result, with masked sensitive values.
	 */
	private HttpHeaders maskSensitiveHeaders(HttpHeaders headers) {

		List<String> keysToMask = this.loggingProperties.getSensitiveOutgoingHeaders();
		if (keysToMask == null || keysToMask.isEmpty()) {
			return headers;
		}

		HttpHeaders map = new HttpHeaders();
		headers.forEach((key, value) -> {
			if (keysToMask.contains(key)) {
				map.put(key, value.stream().map(v -> "*".repeat(v.length())).collect(Collectors.toList()));
			} else {
				map.put(key, value);
			}
		});
		return map;
	}

	private String maskSensitiveParamters(URI uri) {
		List<String> keysToMask = this.loggingProperties.getSensitiveRequestParameters();

		String result = uri.toString();
		if (StringUtils.isEmpty(uri.getRawQuery()) || keysToMask == null || keysToMask.isEmpty()) {
			return result;
		}

		String[] params = uri.getRawQuery().split("&");
		for (int i = 0; i < params.length; i++) {
			String param = params[i];
			String[] keyValue = param.split("=");
			String key = keyValue[0];
			if (keyValue.length == 2) {
				String value = keyValue[1];
				if (keysToMask.contains(key)) {
					String newParam = key + "=" + "*".repeat(value.length());
					result = result.replace("?" + param, "?" + newParam);
					result = result.replace("&" + param, "&" + newParam);
				}
			}
		}
		return result;
	}
}
