package de.enterprise.starters.aws.ecs.sqs;

import java.util.List;

import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory;
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.cloud.aws.messaging.config.annotation.SqsConfiguration;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;

import de.enterprise.spring.boot.application.starter.tracing.TracingProperties;
import de.enterprise.starters.aws.ecs.sqs.SqsProperties.Defaults.Request;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * Pretty much copy and paste from {@link SimpleMessageListenerContainerFactory}. Necessary, because we want to create an
 * {@link ResilientMessageListenerContainer} instead of an {@link SimpleMessageListenerContainer}.
 *
 * Being available as bean, it is automatically used in {@link SqsConfiguration}.
 *
 * @author Jonas Keßler
 */
public class ResilientMessageListenerContainerFactory extends SimpleMessageListenerContainerFactory {

	private AsyncTaskExecutor taskExecutor;

	private boolean autoStartup = true;

	private AmazonSQSAsync amazonSqs;

	private QueueMessageHandler queueMessageHandler;

	private ResourceIdResolver resourceIdResolver;

	private DestinationResolver<String> destinationResolver;

	private final TracingProperties tracingProperties;
	private final SqsProperties sqsProperties;
	private final CircuitBreakerRegistry circuitBreakerRegistry;

	private ResilientMessageListenerContainer containerInstance;

	private final List<MessageListenerContainerEventListener> eventListeners;

	public ResilientMessageListenerContainerFactory(SqsProperties sqsProperties, CircuitBreakerRegistry circuitBreakerRegistry,
			List<MessageListenerContainerEventListener> eventListeners, TracingProperties tracingProperties) {
		super();
		this.sqsProperties = sqsProperties;
		this.tracingProperties = tracingProperties;
		this.circuitBreakerRegistry = circuitBreakerRegistry;
		this.eventListeners = eventListeners;
	}

	/**
	 * Configures the {@link TaskExecutor} which is used to poll messages and execute them by calling the handler methods. If no
	 * {@link TaskExecutor} is set, a default one is created.
	 *
	 * @param taskExecutor
	 *            The {@link TaskExecutor} used by the container
	 * @see SimpleMessageListenerContainer#createDefaultTaskExecutor()
	 */
	@Override
	public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Configures if this container should be automatically started. The default value is true.
	 *
	 * @param autoStartup
	 *            - false if the container will be manually started
	 */
	@Override
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * Sets the {@link AmazonSQSAsync} that is going to be used by the container to interact with the messaging (SQS) API.
	 *
	 * @param amazonSqs
	 *            The {@link AmazonSQSAsync}, must not be {@code null}.
	 */
	@Override
	public void setAmazonSqs(AmazonSQSAsync amazonSqs) {
		Assert.notNull(amazonSqs, "amazonSqs must not be null");
		this.amazonSqs = amazonSqs;
	}

	@Override
	public AmazonSQS getAmazonSqs() {
		return this.amazonSqs;
	}

	/**
	 * Configures the {@link QueueMessageHandler} that must be used to handle incoming messages.
	 * <p>
	 * <b>NOTE</b>: It is rather unlikely that the {@link QueueMessageHandler} must be configured with this setter. Consider using the
	 * {@link QueueMessageHandlerFactory} to configure the {@link QueueMessageHandler} before using this setter.
	 * </p>
	 *
	 * @param messageHandler
	 *            the {@link QueueMessageHandler} that must be used by the container, must not be {@code null}.
	 * @see QueueMessageHandlerFactory
	 */
	@Override
	public void setQueueMessageHandler(QueueMessageHandler messageHandler) {
		Assert.notNull(messageHandler, "messageHandler must not be null");
		this.queueMessageHandler = messageHandler;
	}

	@Override
	public QueueMessageHandler getQueueMessageHandler() {
		return this.queueMessageHandler;
	}

	/**
	 * This value must be set if no destination resolver has been set.
	 *
	 * @param resourceIdResolver
	 *            the resourceIdResolver to use for resolving logical to physical ids in a CloudFormation environment. Must not be null.
	 */
	@Override
	public void setResourceIdResolver(ResourceIdResolver resourceIdResolver) {
		this.resourceIdResolver = resourceIdResolver;
	}

	@Override
	public ResourceIdResolver getResourceIdResolver() {
		return this.resourceIdResolver;
	}

	/**
	 * Configures the destination resolver used to retrieve the queue url based on the destination name configured for this instance. <br/>
	 * This setter can be used when a custom configured {@link DestinationResolver} must be provided. (For example if one want to have the
	 * {@link org.springframework.cloud.aws.messaging.support.destination.DynamicQueueUrlDestinationResolver} with the auto creation of
	 * queues set to {@code true}.
	 *
	 * @param destinationResolver
	 *            another or customized {@link DestinationResolver}
	 */
	@Override
	public void setDestinationResolver(DestinationResolver<String> destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	/**
	 * Created container instance.
	 *
	 * @return List of all created container instances.
	 */
	public ResilientMessageListenerContainer getContainerInstance() {
		return this.containerInstance;
	}

	@Override
	public SimpleMessageListenerContainer createSimpleMessageListenerContainer() {
		Assert.notNull(this.amazonSqs, "amazonSqs must not be null");

		ResilientMessageListenerContainer messageListenerContainer = new ResilientMessageListenerContainer(this.circuitBreakerRegistry,
				this.sqsProperties, this.eventListeners, this.tracingProperties);
		messageListenerContainer.setAmazonSqs(this.amazonSqs);
		messageListenerContainer.setAutoStartup(this.autoStartup);

		if (this.taskExecutor != null) {
			messageListenerContainer.setTaskExecutor(this.taskExecutor);
		}
		if (this.resourceIdResolver != null) {
			messageListenerContainer.setResourceIdResolver(this.resourceIdResolver);
		}
		if (this.destinationResolver != null) {
			messageListenerContainer.setDestinationResolver(this.destinationResolver);
		}

		Request requestProperties = this.sqsProperties.getDefaults().getRequest();
		if (requestProperties != null) {
			if (requestProperties.getMaxNumberOfMessages() != null) {
				messageListenerContainer.setMaxNumberOfMessages(requestProperties.getMaxNumberOfMessages());
			}
			if (requestProperties.getVisibilityTimeout() != null) {
				messageListenerContainer.setVisibilityTimeout(requestProperties.getVisibilityTimeout());
			}
			if (requestProperties.getWaitTimeOut() != null) {
				messageListenerContainer.setWaitTimeOut(requestProperties.getWaitTimeOut());
			}
		}
		messageListenerContainer.setBackOffTime(this.sqsProperties.getDefaults().getBackOffTime() * 1000);
		this.containerInstance = messageListenerContainer;
		return messageListenerContainer;
	}
}
