package de.enterprise.starters.aws.ecs.sqs;

import java.util.List;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.messaging.config.annotation.EnableSqs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.enterprise.spring.boot.application.starter.tracing.TracingProperties;
import de.enterprise.starters.aws.ecs.sqs.actuate.MessageListenerMetricsPublisher;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.micrometer.core.instrument.MeterRegistry;

/**
 *
 *
 * @author Jonas Keßler
 */
@Configuration
@ConditionalOnClass(EnableSqs.class)
@EnableConfigurationProperties(SqsProperties.class)
@AutoConfigureAfter({ CircuitBreakerAutoConfiguration.class, MetricsAutoConfiguration.class })
public class SqsAutoConfiguration {

	@Bean
	public MessageListenerContainerEventListener messageListenerContainerEventListener(MeterRegistry meterRegistry) {
		return new MessageListenerMetricsPublisher(meterRegistry);
	}

	@Bean
	public ResilientMessageListenerContainerFactory resilientMessageListenerContainerFactory(SqsProperties properties,
			CircuitBreakerRegistry circuitBreakerRegistry, List<MessageListenerContainerEventListener> eventListener,
			TracingProperties tracingProperties) {
		return new ResilientMessageListenerContainerFactory(properties, circuitBreakerRegistry, eventListener, tracingProperties);
	}

}
