package de.enterprise.starters.aws.ecs.sqs;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.Data;

/**
 *
 *
 * @author Jonas Keßler
 */
@Data
@Validated
@ConfigurationProperties("enterprise-application.sqs")
public class SqsProperties {

	@Autowired
	private final CircuitBreakerRegistry circuitBreakerRegistry;

	/**
	 * Maps internal queue name name to specific queue properties.
	 */
	@Valid
	@NotNull
	private Map<String, QueueProperties> queues = new HashMap<>();

	@Valid
	@NotNull
	private Defaults defaults = new Defaults();

	@SuppressWarnings("unused") // TODO needed for PMD
	@AssertTrue(message = "CircuitBreaker configuration is needed for each sqs queue.")
	private boolean isValid() {

		return this.getQueues().keySet().stream()
				.map(k -> ResilientMessageListenerContainer.createCircuitBreakerName(k))
				.map(k -> this.circuitBreakerRegistry.getAllCircuitBreakers().toJavaStream().anyMatch(cb -> cb.getName().equals(k)))
				.allMatch(b -> b.booleanValue());
	}

	public Optional<Map.Entry<String, QueueProperties>> findInternalNameByLogicalQueueName(String logicalQueueName) {
		return this.queues.entrySet().stream().filter(e -> e.getValue().getName().equals(logicalQueueName)).findAny();
	}

	/**
	 * Default properties which are used for all queues and can not be changed per queue.
	 *
	 * @author Jonas Keßler
	 */
	@Data
	public static class Defaults {

		/**
		 * The number of seconds the polling thread must wait before trying to recover when an error occurs while requesting sqs (e.g.
		 * connection timeout).
		 */
		@Min(0)
		private long backOffTime = 10;

		@NotNull
		@Valid
		private Request request = new Request();

		/**
		 * Properties used for SQS-Requests. If null, default configured with the queue are used.
		 *
		 * @author Jonas Keßler
		 */
		@Data
		public static class Request {
			/**
			 * Configure the maximum number of messages that should be retrieved during one poll to the Amazon SQS system. This number must
			 * be a positive, non-zero number that has a maximum number of 10. Values higher then 10 are currently not supported by the
			 * queueing system.
			 */
			@Min(1)
			@Max(10)
			private Integer maxNumberOfMessages = 10;

			/**
			 * Configures the duration (in seconds) that the received messages are hidden from subsequent poll requests after being
			 * retrieved from the system.
			 */
			@Min(0)
			private Integer visibilityTimeout = 30;

			/**
			 * Configures the wait timeout that the poll request will wait for new message to arrive if the are currently no messages on the
			 * queue. Higher values will reduce poll request to the system significantly. The value should be between 1 and 20. For more
			 * information read the
			 * <a href="http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-long-polling.html">documentation</a>.
			 */
			@Min(0)
			@Max(20)
			private Integer waitTimeOut = 20;
		}
	}

	/**
	 * Specific sqs queue propertis.
	 *
	 * @author Jonas Keßler
	 */
	@Data
	public static class QueueProperties {

		/**
		 * External queue name.
		 */
		@NotNull
		private String name;

		/**
		 * Max age of message before it gets deleted by application. Maxmimum age defined here should be lower than sqs property "Message
		 * Retention Period" defined at queue creation time. Otherwise messages will always be automatically deleted without being able to
		 * trigger an action inside the applciation.
		 *
		 * @see member {@link #deleteMessagesOnMaxAgeEnabled}
		 */
		@NotNull
		@DurationUnit(ChronoUnit.DAYS)
		private Duration maxAge = Duration.of(10L, ChronoUnit.DAYS);

		/**
		 * @see member {@link #maxAge}
		 */
		@NotNull
		private Boolean deleteMessagesOnMaxAgeEnabled = true;

	}
}
