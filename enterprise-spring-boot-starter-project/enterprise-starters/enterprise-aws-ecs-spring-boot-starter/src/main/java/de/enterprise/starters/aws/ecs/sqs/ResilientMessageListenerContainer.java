package de.enterprise.starters.aws.ecs.sqs;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.springframework.cloud.aws.messaging.core.QueueMessageUtils;
import org.springframework.cloud.aws.messaging.listener.QueueMessageAcknowledgment;
import org.springframework.cloud.aws.messaging.listener.QueueMessageVisibility;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;

import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

import de.enterprise.spring.boot.application.starter.tracing.TracingProperties;
import de.enterprise.spring.boot.application.starter.tracing.TracingUtils;
import de.enterprise.starters.aws.ecs.sqs.SqsProperties.QueueProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * As there is very much private in {@link SimpleMessageListenerContainer}, we have to copy almost everything in order to apply the
 * "robustness".
 *
 * Robust means that the container slows down the polling mechanism in case of errors. Logic how to act in case of errors is managed by
 * {@link FailsafeCircuitBreaker}. Each sqs queue used in here has its own idependent circuitbreaker, but for now they all share the same
 * settings.
 *
 * TODO: use logger from @Slf4j ?
 *
 * @author Jonas Keßler
 */
public class ResilientMessageListenerContainer extends SimpleMessageListenerContainer {

	private static final String CIRCUITBREAKER_NAME_PREFIX = "sqsListener";
	private final CircuitBreakerRegistry circuitBreakerRegistry;
	private final SqsProperties sqsDefaultProperties;
	private final TracingProperties tracingProperties;
	private final Map<String, String> externalToInternalQueueName = new HashMap<>();
	private final List<MessageListenerContainerEventListener> eventListeners;

	// copied from QueueMessageHandler, as not visible here
	static final String LOGICAL_RESOURCE_ID = "LogicalResourceId";
	static final String ACKNOWLEDGMENT = "Acknowledgment";
	static final String VISIBILITY = "Visibility";

	public ResilientMessageListenerContainer(CircuitBreakerRegistry circuitBreakerRegistry, SqsProperties circuitBreakerProperties,
			List<MessageListenerContainerEventListener> eventListeners, TracingProperties tracingProperties) {
		this.circuitBreakerRegistry = circuitBreakerRegistry;
		this.sqsDefaultProperties = circuitBreakerProperties;
		this.eventListeners = eventListeners;
		this.tracingProperties = tracingProperties;
	}

	/*
	 * following fields copied from SimpleMessageListenerContainer
	 */
	private long queueStopTimeout = 10000;

	private ConcurrentHashMap<String, Future<?>> scheduledFutureByQueue;
	private ConcurrentHashMap<String, Boolean> runningStateByQueue;

	public static String createCircuitBreakerName(String queueName) {
		String pascalCaseQueueName = queueName.substring(0, 1).toUpperCase() + queueName.substring(1);
		return CIRCUITBREAKER_NAME_PREFIX + pascalCaseQueueName;
	}

	@Override
	protected void initialize() {
		super.initialize();

		initializeRunningStateByQueue();
		this.scheduledFutureByQueue = new ConcurrentHashMap<>(getRegisteredQueues().size());
	}

	private void initializeRunningStateByQueue() {
		this.runningStateByQueue = new ConcurrentHashMap<>(getRegisteredQueues().size());
		for (String queueName : getRegisteredQueues().keySet()) {
			this.runningStateByQueue.put(queueName, false);
		}
	}

	@Override
	protected void doStart() {
		synchronized (this.getLifecycleMonitor()) {
			scheduleMessageListeners();
		}
	}

	@Override
	protected void doStop() {
		notifyRunningQueuesToStop();
		waitForRunningQueuesToStop();
	}

	private void notifyRunningQueuesToStop() {
		for (Map.Entry<String, Boolean> runningStateByQueue : this.runningStateByQueue.entrySet()) {
			if (runningStateByQueue.getValue()) {
				stopQueue(runningStateByQueue.getKey());
			}
		}
	}

	private void waitForRunningQueuesToStop() {
		for (Map.Entry<String, Boolean> queueRunningState : this.runningStateByQueue.entrySet()) {
			String logicalQueueName = queueRunningState.getKey();
			Future<?> queueSpinningThread = this.scheduledFutureByQueue.get(logicalQueueName);

			if (queueSpinningThread != null) {
				try {
					queueSpinningThread.get(getQueueStopTimeout(), TimeUnit.SECONDS);
				} catch (ExecutionException | TimeoutException e) {
					getLogger().warn("An exception occurred while stopping queue '" + logicalQueueName + "'", e);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	private void scheduleMessageListeners() {
		for (Map.Entry<String, QueueAttributes> registeredQueue : getRegisteredQueues().entrySet()) {
			startQueue(registeredQueue.getKey(), registeredQueue.getValue());
		}
	}

	@Override
	protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
		getMessageHandler().handleMessage(stringMessage);
	}

	/**
	 * Stops and waits until the specified queue has stopped. If the wait timeout specified by
	 * {@link SimpleMessageListenerContainer#getQueueStopTimeout()} is reached, the current thread is interrupted.
	 *
	 * @param logicalQueueName
	 *            the name as defined on the listener method
	 */
	@Override
	public void stop(String logicalQueueName) {
		stopQueue(logicalQueueName);

		try {
			if (isRunning(logicalQueueName)) {
				Future<?> future = this.scheduledFutureByQueue.remove(logicalQueueName);
				if (future != null) {
					future.get(this.queueStopTimeout, TimeUnit.MILLISECONDS);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException | TimeoutException e) {
			getLogger().warn("Error stopping queue with name: '" + logicalQueueName + "'", e);
		}
	}

	@Override
	protected void stopQueue(String logicalQueueName) {
		Assert.isTrue(this.runningStateByQueue.containsKey(logicalQueueName), "Queue with name '" + logicalQueueName + "' does not exist");
		this.runningStateByQueue.put(logicalQueueName, false);
	}

	@Override
	public void start(String logicalQueueName) {
		Assert.isTrue(this.runningStateByQueue.containsKey(logicalQueueName), "Queue with name '" + logicalQueueName + "' does not exist");

		QueueAttributes queueAttributes = this.getRegisteredQueues().get(logicalQueueName);
		startQueue(logicalQueueName, queueAttributes);
	}

	/**
	 * Checks if the spinning thread for the specified queue {@code logicalQueueName} is still running (polling for new messages) or not.
	 *
	 * @param logicalQueueName
	 *            the name as defined on the listener method
	 * @return {@code true} if the spinning thread for the specified queue is running otherwise {@code false}.
	 */
	@Override
	public boolean isRunning(String logicalQueueName) {
		Future<?> future = this.scheduledFutureByQueue.get(logicalQueueName);
		return future != null && !future.isCancelled() && !future.isDone();
	}

	@Override
	protected void startQueue(String queueName, QueueAttributes queueAttributes) {
		if (this.runningStateByQueue.containsKey(queueName) && this.runningStateByQueue.get(queueName)) {
			return;
		}

		this.runningStateByQueue.put(queueName, true);

		Optional<Entry<String, QueueProperties>> queuePropertiesOptional = this.sqsDefaultProperties
				.findInternalNameByLogicalQueueName(queueName);
		String internalQueueName = queuePropertiesOptional
				.orElseThrow(
						() -> new IllegalArgumentException(
								"Queue with queueName=" + queueName + " used in @SqsListener annotation, but no QueueProperties defined!"))
				.getKey();
		this.externalToInternalQueueName.put(queueName, internalQueueName);
		String circuitBreakerName = createCircuitBreakerName(internalQueueName);
		CircuitBreaker circuitBreaker = this.circuitBreakerRegistry.circuitBreaker(circuitBreakerName);

		Future<?> future = getTaskExecutor().submit(new AsynchronousMessageListener(queueName, queueAttributes, circuitBreaker));
		this.scheduledFutureByQueue.put(queueName, future);
	}

	private void notifyEventListeners(Consumer<? super MessageListenerContainerEventListener> consumer) {
		this.eventListeners.forEach(consumer);
	}

	private final class AsynchronousMessageListener implements Runnable {

		private final QueueAttributes queueAttributes;
		private final String logicalQueueName;
		private final String internalQueueName;
		private final CircuitBreaker circuitBreaker;

		private AsynchronousMessageListener(String logicalQueueName, QueueAttributes queueAttributes, CircuitBreaker circuitBreaker) {
			this.logicalQueueName = logicalQueueName;
			this.queueAttributes = queueAttributes;
			this.circuitBreaker = circuitBreaker;
			this.internalQueueName = ResilientMessageListenerContainer.this.externalToInternalQueueName.get(this.logicalQueueName);
		}

		@Override
		public void run() {
			while (isQueueRunning()) {
				try {
					ReceiveMessageRequest request = this.queueAttributes.getReceiveMessageRequest();

					// sleep a while if circuit is opened
					if (!this.circuitBreaker.tryAcquirePermission()) {

						try {
							int sleep = 1000;
							getLogger().trace("sleep for {} ms as calls are permitted by circuitBreakerName={} queueName={}", sleep,
									this.circuitBreaker.getName(), this.logicalQueueName);
							notifyEventListeners(l -> l.onSqsRequestRejection(this.internalQueueName));
							Thread.sleep(sleep);
						} catch (InterruptedException ie) {
							Thread.currentThread().interrupt();
						}
						continue;
					}

					getLogger().trace(
							"About to request SQS with queueName={} maxNumberOfMessages={}, circuitBreakerName={}, circuitBreakerState={}",
							this.logicalQueueName, request.getMaxNumberOfMessages(), this.circuitBreaker.getName(),
							this.circuitBreaker.getState());
					notifyEventListeners(l -> l.onSqsRequestAttempt(this.internalQueueName));

					ReceiveMessageResult receiveMessageResult = getAmazonSqs().receiveMessage(request);

					int receivedMessagesCount = receiveMessageResult.getMessages().size();
					getLogger().trace("Received {} messages from queueName={}", receivedMessagesCount, this.logicalQueueName);
					notifyEventListeners(l -> l.onSqsRequestSuccess(this.internalQueueName, receivedMessagesCount));

					CountDownLatch messageBatchLatch = new CountDownLatch(receivedMessagesCount);
					for (Message message : receiveMessageResult.getMessages()) {
						if (isQueueRunning()) {
							MessageExecutor messageExecutor = new MessageExecutor(this.logicalQueueName, message, this.queueAttributes,
									this.circuitBreaker);
							getTaskExecutor().execute(new SignalExecutingRunnable(messageBatchLatch, messageExecutor));
						} else {
							messageBatchLatch.countDown();
						}
					}
					try {
						messageBatchLatch.await();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}

				} catch (Exception e) {
					getLogger().warn("An Exception occurred while polling queue '{}'. The failing operation will be " +
							"retried in {} milliseconds", this.logicalQueueName, getBackOffTime(), e);
					notifyEventListeners(l -> l.onSqsRequestFailure(this.internalQueueName, e));
					try {
						// noinspection BusyWait
						Thread.sleep(getBackOffTime());
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
				}
			}

			ResilientMessageListenerContainer.this.scheduledFutureByQueue.remove(this.logicalQueueName);
		}

		private boolean isQueueRunning() {
			if (ResilientMessageListenerContainer.this.runningStateByQueue.containsKey(this.logicalQueueName)) {
				return ResilientMessageListenerContainer.this.runningStateByQueue.get(this.logicalQueueName);
			} else {
				getLogger().warn("Stopped queue '" + this.logicalQueueName + "' because it was not listed as running queue.");
				return false;
			}
		}
	}

	private final class MessageExecutor implements Runnable {

		private final Message message;
		private final String logicalQueueName;
		private final String queueUrl;
		private final boolean hasRedrivePolicy;
		private final SqsMessageDeletionPolicy deletionPolicy;
		private final CircuitBreaker circuitBreaker;
		private final QueueProperties queueProperties;
		private final String internalQueueName;
		private final TracingProperties tracingProperties;

		private MessageExecutor(String logicalQueueName, Message message, QueueAttributes queueAttributes, CircuitBreaker circuitBreaker) {
			this.logicalQueueName = logicalQueueName;
			this.message = message;
			this.queueUrl = queueAttributes.getReceiveMessageRequest().getQueueUrl();
			this.hasRedrivePolicy = queueAttributes.hasRedrivePolicy();
			// hardcode deletion policy to ON_SUCCESS
			this.deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS;
			this.circuitBreaker = circuitBreaker;
			this.internalQueueName = ResilientMessageListenerContainer.this.externalToInternalQueueName.get(this.logicalQueueName);
			this.queueProperties = ResilientMessageListenerContainer.this.sqsDefaultProperties.getQueues().get(this.internalQueueName);
			this.tracingProperties = ResilientMessageListenerContainer.this.tracingProperties;
		}

		@Override
		public void run() {
			String receiptHandle = this.message.getReceiptHandle();

			// delete message if internal maxAge is reached
			if (this.queueProperties.getDeleteMessagesOnMaxAgeEnabled() && maxAgeReached()) {
				applyDeletionPolicyOnSuccess(receiptHandle);
				notifyEventListeners(l -> l.onMessageDeletionMaxAgeReached(this.internalQueueName));
				return;
			}

			org.springframework.messaging.Message<String> queueMessage = getMessageForExecution();
			try {
				TracingUtils.addNewMdcTraceContext(this.tracingProperties);
				if (this.circuitBreaker.tryAcquirePermission()) {
					notifyEventListeners(l -> l.onMessageProcessingAttempt(this.internalQueueName));
					this.circuitBreaker.executeRunnable(() -> executeMessage(queueMessage));
					applyDeletionPolicyOnSuccess(receiptHandle);
					notifyEventListeners(l -> l.onMessageProcessingSuccess(this.internalQueueName));
				} else {
					getLogger().debug(
							"skipping processing of received sqs-message as circuitbreaker allows no more executions queueName={}, circuitBreakerName={}",
							this.logicalQueueName, this.circuitBreaker.getName());
					notifyEventListeners(l -> l.onMessageProcessingRejection(this.internalQueueName));
				}
			} catch (MessagingException messagingException) {
				getLogger().debug("exception while processing message for queueName={}, circuitBreakerName={}", this.logicalQueueName,
						this.circuitBreaker.getName());
				notifyEventListeners(l -> l.onMessageProcessingFailure(this.internalQueueName));

				// if the message format is invalid delete it from queue directly, handle it the standard way otherwise
				if (messagingException.getCause() instanceof MessageConversionException) {
					getLogger().error("Exception while converting message. Message is being deleted from the queue.", messagingException);
					deleteMessage(receiptHandle);
					notifyEventListeners(l -> l.onMessageDeletionConversionError(this.internalQueueName,
							(MessageConversionException) messagingException.getCause()));
				} else {
					applyDeletionPolicyOnError(receiptHandle, messagingException);
				}
			} finally {
				TracingUtils.removeMdcTraceContext(this.tracingProperties);
			}
		}

		private void applyDeletionPolicyOnSuccess(String receiptHandle) {
			if (this.deletionPolicy == SqsMessageDeletionPolicy.ON_SUCCESS ||
					this.deletionPolicy == SqsMessageDeletionPolicy.ALWAYS ||
					this.deletionPolicy == SqsMessageDeletionPolicy.NO_REDRIVE) {
				deleteMessage(receiptHandle);
			}
		}

		private void applyDeletionPolicyOnError(String receiptHandle, Exception messagingException) {
			if (this.deletionPolicy == SqsMessageDeletionPolicy.ALWAYS ||
					this.deletionPolicy == SqsMessageDeletionPolicy.NO_REDRIVE && !this.hasRedrivePolicy) {
				deleteMessage(receiptHandle);
			} else if (this.deletionPolicy == SqsMessageDeletionPolicy.ON_SUCCESS) {
				getLogger().warn("Exception encountered while processing message. Message is not being deleted from the queue.",
						messagingException);
			}
		}

		private void deleteMessage(String receiptHandle) {
			getAmazonSqs().deleteMessageAsync(new DeleteMessageRequest(this.queueUrl, receiptHandle));
		}

		private org.springframework.messaging.Message<String> getMessageForExecution() {
			HashMap<String, Object> additionalHeaders = new HashMap<>();
			additionalHeaders.putIfAbsent(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE);
			additionalHeaders.put(LOGICAL_RESOURCE_ID, this.logicalQueueName);
			if (this.deletionPolicy == SqsMessageDeletionPolicy.NEVER) {
				String receiptHandle = this.message.getReceiptHandle();
				QueueMessageAcknowledgment acknowledgment = new QueueMessageAcknowledgment(
						ResilientMessageListenerContainer.this.getAmazonSqs(), this.queueUrl, receiptHandle);
				additionalHeaders.put(ACKNOWLEDGMENT, acknowledgment);
			}
			additionalHeaders.put(VISIBILITY, new QueueMessageVisibility(
					ResilientMessageListenerContainer.this.getAmazonSqs(), this.queueUrl, this.message.getReceiptHandle()));

			return QueueMessageUtils.createMessage(this.message, additionalHeaders);
		}

		private boolean maxAgeReached() {

			String sentTimeStamp = this.message.getAttributes().get("SentTimestamp");
			if (sentTimeStamp != null) {
				long epochMillis = Long.parseLong(sentTimeStamp);
				Instant sentInstant = Instant.ofEpochMilli(epochMillis);

				Instant maxAgeInstant = sentInstant.plus(this.queueProperties.getMaxAge());
				if (maxAgeInstant.isBefore(Instant.now())) {
					getLogger().error("Sqs message has reached max age - queueName={}, UTC-sentDateTime={}, body={}, attributes={}",
							this.logicalQueueName, sentInstant.atZone(ZoneOffset.UTC).toLocalDateTime(), this.message.getBody(),
							this.message.getAttributes());
					return true;
				}
			}
			return false;
		}
	}

	private final static class SignalExecutingRunnable implements Runnable {

		private final CountDownLatch countDownLatch;
		private final Runnable runnable;

		private SignalExecutingRunnable(CountDownLatch endSignal, Runnable runnable) {
			this.countDownLatch = endSignal;
			this.runnable = runnable;
		}

		@Override
		public void run() {
			try {
				this.runnable.run();
			} finally {
				this.countDownLatch.countDown();
			}
		}
	}

}
