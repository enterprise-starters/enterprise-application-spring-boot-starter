package de.enterprise.starters.aws.ecs.sqs;

import org.springframework.messaging.converter.MessageConversionException;

import de.enterprise.starters.aws.ecs.sqs.SqsProperties.QueueProperties;

/**
 * 
 *
 * @author Jonas Keßler
 */
public interface MessageListenerContainerEventListener {

	/**
	 * Called before request to sqs is made.
	 * 
	 * @param internalQueueName
	 *            the internal queue representation
	 */
	void onSqsRequestAttempt(String internalQueueName);

	/**
	 * Called if request to sqs is rejected by the circuitbreaker.
	 * 
	 * @param internalQueueName
	 *            the internal queue representation
	 */
	void onSqsRequestRejection(String internalQueueName);

	/**
	 * Called on successful sqs request.
	 * 
	 * @param internalQueueName
	 *            the internal queue representation
	 * @param messagesCount
	 *            count of received messages
	 */
	void onSqsRequestSuccess(String internalQueueName, int messagesCount);

	/**
	 * Called on failed sqs request - if request terminated with an exception.
	 * 
	 * @param internalQueueName
	 *            the internal queue representation
	 * @param ex
	 *            exception which was thrown
	 */
	void onSqsRequestFailure(String internalQueueName, Exception ex);

	/**
	 * Called before processing of a single message is started.
	 * 
	 * @param internalQueueName
	 *            the internal queue representation
	 */
	void onMessageProcessingAttempt(String internalQueueName);

	/**
	 * Called if processing of a single message is rejected by circuitbreaker.
	 * 
	 * @param internalQueueName
	 *            the internal queue representation
	 */
	void onMessageProcessingRejection(String internalQueueName);

	/**
	 * Called on a successful processing of a single message.
	 * 
	 * @param internalQueueName
	 *            the internal queue representation
	 */
	void onMessageProcessingSuccess(String internalQueueName);

	/**
	 * 
	 * @param internalQueueName
	 *            the internal queue representation
	 */
	void onMessageProcessingFailure(String internalQueueName);

	/**
	 * Called if a message was deleted, because it's {@link QueueProperties#maxAge} was reached.
	 * 
	 * @param internalQueueName
	 *            the internal queue representation
	 */
	void onMessageDeletionMaxAgeReached(String internalQueueName);

	/**
	 * Called if a message was deleted, because of a conversion exception.
	 * 
	 * @param internalQueueName
	 *            the internal queue representation
	 * @param ex
	 *            exception which was thrown
	 */
	void onMessageDeletionConversionError(String internalQueueName, MessageConversionException ex);

}
