package de.enterprise.spring.boot.application.starter.logging;

import java.util.Map;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * @author Malte Geßner
 *
 */
public class MdcTaskDecorator implements TaskDecorator {
	@Override
	public Runnable decorate(Runnable runnable) {
		// Right now: Web thread context !
		// (Grab the current thread MDC data)
		Map<String, String> contextMap = MDC.getCopyOfContextMap();
		return () -> {
			try {
				// Right now: @Async thread context !
				// (Restore the Web thread context's MDC data)
				if (contextMap != null) {
					MDC.setContextMap(contextMap);
				}
				runnable.run();
			} finally {
				MDC.clear();
			}
		};
	}
}
