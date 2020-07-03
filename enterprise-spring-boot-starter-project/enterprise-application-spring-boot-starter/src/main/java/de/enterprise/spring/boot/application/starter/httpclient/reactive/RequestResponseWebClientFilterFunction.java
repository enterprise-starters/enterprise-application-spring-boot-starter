package de.enterprise.spring.boot.application.starter.httpclient.reactive;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.actuate.metrics.web.reactive.client.MetricsWebClientCustomizer;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import de.enterprise.spring.boot.application.starter.logging.LoggingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * {@link ExchangeFilterFunction} applied via a {@link MetricsWebClientCustomizer} to record metrics.
 *
 * @author Brian Clozel
 * @author Tadaya Tsuyukubo
 * @since 2.1.0
 */
@Slf4j(topic = "request-logger-outbound")
@RequiredArgsConstructor
public class RequestResponseWebClientFilterFunction implements ExchangeFilterFunction {
	private static final String VALUE_SEPARATOR = "; ";
	private static final String REQUEST_RESPONSE_WEBCLIENT_REQUEST_UUID = RequestResponseWebClientFilterFunction.class.getName()
			+ ".REQUEST_UUID";
	private static final String REQUEST_RESPONSE_WEBCLIENT_REQUEST_DETAILS = RequestResponseWebClientFilterFunction.class.getName()
			+ ".REQUEST_DETAILS";

	private final LoggingProperties loggingProperties;

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		String requestUuid = UUID.randomUUID().toString();
		String requestDetails = logRequest(requestUuid, request, "".getBytes());

		return next.exchange(request).doOnEach((signal) -> {
			if (!signal.isOnComplete()) {
				ClientResponse response = signal.get();
				String localRequestUuid = signal.getContext().get(REQUEST_RESPONSE_WEBCLIENT_REQUEST_UUID);
				String localRequestDetails = signal.getContext().get(REQUEST_RESPONSE_WEBCLIENT_REQUEST_DETAILS);

				try {
					logResponse(localRequestUuid, localRequestDetails, response);
				} catch (IOException e) {
					log.error("client response logging failed", e);
				}
			}
		}).subscriberContext(context -> this.putRequestData(context, requestUuid, requestDetails));
	}

	private Context putRequestData(Context context, String requestUuid, String requestDetails) {

		context = context.put(REQUEST_RESPONSE_WEBCLIENT_REQUEST_UUID, requestUuid);
		return context.put(REQUEST_RESPONSE_WEBCLIENT_REQUEST_DETAILS, requestDetails);
	}

	private String logRequest(String requestUuid, ClientRequest request, byte[] body) {
		String requestDetails = "";
		if (log.isInfoEnabled()) {
			StringBuilder requestDetailsBuilder = new StringBuilder();
			requestDetailsBuilder.append("method=").append(request.method()).append(VALUE_SEPARATOR)
					.append("uri=").append(maskSensitiveParamters(request.url()));
			requestDetails = requestDetailsBuilder.toString();

			StringBuilder msg = new StringBuilder();
			msg.append("Outgoing REST request with requestUuid=").append(requestUuid).append(VALUE_SEPARATOR)
					.append(requestDetails).append(VALUE_SEPARATOR)
					.append("headers=").append(maskSensitiveHeaders(request.headers()));
			if (this.loggingProperties.isLogOutgoingRequestWithPayload()) {
				boolean multipartBody = request.headers().getContentType() != null
						&& "multipart".equals(request.headers().getContentType().getType());
				if (!multipartBody && body.length > 0) {
					try {
						msg.append(VALUE_SEPARATOR).append("requestBody=").append(new String(body, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						log.error("can't convert body to string", e);
					}
				}
			}
			log.info(msg.toString());
		}

		return requestDetails;
	}

	private void logResponse(String requestUuid, String requestDetails, ClientResponse response) throws IOException {
		if (log.isInfoEnabled()) {
			StringBuilder msg = new StringBuilder();
			msg.append("Incoming REST response with requestUuid=").append(requestUuid).append(VALUE_SEPARATOR)
					.append(requestDetails).append(VALUE_SEPARATOR)
					.append("statusCode=").append(response.statusCode()).append(VALUE_SEPARATOR)
					.append("headers=").append(response.headers());

			response.toEntity(String.class).doOnNext(responseEntity -> {
				if (this.loggingProperties.isLogOutgoingRequestWithPayload()) {
					String responseBody = responseEntity.getBody();
					if (!StringUtils.isBlank(responseBody)) {
						msg.append(VALUE_SEPARATOR).append("responseBody=").append(responseBody);
					}
				}
				log.info(msg.toString());
			});
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
