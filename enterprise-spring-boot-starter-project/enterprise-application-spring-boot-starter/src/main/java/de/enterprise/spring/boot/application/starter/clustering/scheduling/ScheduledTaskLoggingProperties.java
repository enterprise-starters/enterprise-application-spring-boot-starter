package de.enterprise.spring.boot.application.starter.clustering.scheduling;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Malte Gessner
 *
 */
@ConfigurationProperties("enterprise-application.scheduling.logging")
@Validated
@Getter
@Setter
public class ScheduledTaskLoggingProperties {

	private boolean enabled = true;
	private String[] ignoredTasks = new String[] { "AsyncQueueHandler-process" };

}
