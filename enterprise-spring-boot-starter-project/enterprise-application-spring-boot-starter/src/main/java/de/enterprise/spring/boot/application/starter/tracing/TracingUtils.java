package de.enterprise.spring.boot.application.starter.tracing;

import java.util.UUID;
import java.util.function.Function;

import org.slf4j.MDC;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Malte Geßner
 *
 */
@Slf4j
public final class TracingUtils {

	private static Function<String, String> uuidGeneratorFunction = applicationName -> applicationName + "-" + UUID.randomUUID().toString();

	private TracingUtils() {

	}

	public static void setUuidGeneratorFunction(Function<String, String> uuidGeneratorFunction) {
		TracingUtils.uuidGeneratorFunction = uuidGeneratorFunction;
	}

	public static String retrieveOrCreate(String traceValue, String applicationName) {
		String traceId = traceValue;

		if (traceId == null) {
			log.debug("no traceId exist, created new one!");
			traceId = uuidGeneratorFunction.apply(applicationName);
		}

		return traceId;
	}

	public static void addNewMdcTraceContext(TracingProperties tracingProperties) {
		addMdcTraceContext(tracingProperties, null);
	}

	public static void addMdcTraceContext(TracingProperties tracingProperties, String currentTraceId) {
		if (tracingProperties != null
				&& tracingProperties.isEnabled()) {
			MDC.put(tracingProperties.getMdcKey(), TracingUtils.retrieveOrCreate(currentTraceId,
					tracingProperties.getApplicationName()));
		}
	}

	public static void removeMdcTraceContext(TracingProperties tracingProperties) {
		if (tracingProperties != null
				&& tracingProperties.isEnabled()) {
			MDC.remove(tracingProperties.getMdcKey());
		}
	}
}
