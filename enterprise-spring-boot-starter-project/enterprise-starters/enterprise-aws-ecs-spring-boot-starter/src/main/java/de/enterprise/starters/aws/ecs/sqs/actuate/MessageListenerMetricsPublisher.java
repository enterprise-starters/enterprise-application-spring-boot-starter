package de.enterprise.starters.aws.ecs.sqs.actuate;

import org.springframework.messaging.converter.MessageConversionException;

import de.enterprise.starters.aws.ecs.sqs.MessageListenerContainerEventListener;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 
 *
 * @author Jonas Keßler
 */
public class MessageListenerMetricsPublisher implements MessageListenerContainerEventListener {

	private static final String TAG_NAME_INTERNAL_QUEUE_NAME = "internalQueueName";

	private final MeterRegistry meterRegistry;

	public MessageListenerMetricsPublisher(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	@Override
	public void onSqsRequestAttempt(String internalQueueName) {
		this.meterRegistry.counter("sqs.requests.attempt", TAG_NAME_INTERNAL_QUEUE_NAME, internalQueueName).increment();
	}

	@Override
	public void onSqsRequestRejection(String internalQueueName) {
		this.meterRegistry.counter("sqs.requests.rejection", TAG_NAME_INTERNAL_QUEUE_NAME, internalQueueName).increment();

	}

	@Override
	public void onSqsRequestSuccess(String internalQueueName, int messagesCount) {
		this.meterRegistry.counter("sqs.requests.success", TAG_NAME_INTERNAL_QUEUE_NAME, internalQueueName).increment();
		this.meterRegistry.counter("sqs.messages.receipt", TAG_NAME_INTERNAL_QUEUE_NAME, internalQueueName).increment(messagesCount);
	}

	@Override
	public void onSqsRequestFailure(String internalQueueName, Exception ex) {
		this.meterRegistry.counter("sqs.requests.failure", TAG_NAME_INTERNAL_QUEUE_NAME, internalQueueName).increment();
	}

	@Override
	public void onMessageProcessingAttempt(String internalQueueName) {
		this.meterRegistry.counter("sqs.messages.processing.attempt", TAG_NAME_INTERNAL_QUEUE_NAME, internalQueueName).increment();
	}

	@Override
	public void onMessageProcessingRejection(String internalQueueName) {
		this.meterRegistry.counter("sqs.messages.processing.rejection", TAG_NAME_INTERNAL_QUEUE_NAME, internalQueueName).increment();
	}

	@Override
	public void onMessageProcessingSuccess(String internalQueueName) {
		this.meterRegistry.counter("sqs.messages.processing.success", TAG_NAME_INTERNAL_QUEUE_NAME, internalQueueName).increment();
	}

	@Override
	public void onMessageProcessingFailure(String internalQueueName) {
		this.meterRegistry.counter("sqs.messages.processing.failure", TAG_NAME_INTERNAL_QUEUE_NAME, internalQueueName).increment();
	}

	@Override
	public void onMessageDeletionMaxAgeReached(String internalQueueName) {
		this.meterRegistry.counter("sqs.messages.deletion.maxage", TAG_NAME_INTERNAL_QUEUE_NAME, internalQueueName).increment();
	}

	@Override
	public void onMessageDeletionConversionError(String internalQueueName, MessageConversionException ex) {
		this.meterRegistry.counter("sqs.messages.deletion.conversionerror", TAG_NAME_INTERNAL_QUEUE_NAME, internalQueueName).increment();
	}

}
