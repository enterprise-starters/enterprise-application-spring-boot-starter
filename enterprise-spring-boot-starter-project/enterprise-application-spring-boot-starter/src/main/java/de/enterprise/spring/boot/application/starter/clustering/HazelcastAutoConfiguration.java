package de.enterprise.spring.boot.application.starter.clustering;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spi.properties.GroupProperty;

import de.enterprise.spring.boot.application.starter.clustering.actuate.HazelcastHealthIndicator;
import de.enterprise.spring.boot.application.starter.clustering.actuate.HazelcastPublicMetrics;
import de.enterprise.spring.boot.application.starter.clustering.discovery.HazelcastDiscoveryConfigurer;
import de.enterprise.spring.boot.application.starter.clustering.discovery.TcpHazelcastDiscoveryConfigurer;
import de.enterprise.spring.boot.application.starter.clustering.scheduling.HazelcastTaskScheduler;
import de.enterprise.spring.boot.application.starter.clustering.scheduling.ScheduledTaskExecutionProtocol;
import de.enterprise.spring.boot.application.starter.logging.MdcTaskDecorator;
import de.enterprise.spring.boot.application.starter.tracing.TracingProperties;
import io.micrometer.core.instrument.MeterRegistry;

/**
 *
 * @author Malte Geßner, Jonas Keßler
 *
 */
@Configuration
@ConditionalOnClass(HazelcastInstance.class)
@EnableConfigurationProperties({ HazelcastProperties.class })
@ComponentScan("de.enterprise.spring.boot.application.starter.clustering.discovery")
@AutoConfigureBefore(TaskExecutionAutoConfiguration.class)
public class HazelcastAutoConfiguration {

	@ConditionalOnProperty(name = "enterprise-application.hazelcast.discovery-type", havingValue = "Tcp")
	@Bean
	TcpHazelcastDiscoveryConfigurer tcpHazelcastDiscoveryConfigurer() {
		return new TcpHazelcastDiscoveryConfigurer();
	}

	@ConditionalOnMissingBean(Config.class)
	@Bean
	Config hazelcastConfig(HazelcastProperties hazelcastProperties, @Autowired List<HazelcastDiscoveryConfigurer> discoveryConfigurers) {
		Config config = new Config();

		// set instance name to group name
		config.setInstanceName(hazelcastProperties.getGroupName());

		// set instance group
		GroupConfig groupConfig = new GroupConfig(hazelcastProperties.getGroupName());
		config.setGroupConfig(groupConfig);

		NetworkConfig networkConfig = config.getNetworkConfig();
		networkConfig.getJoin().getMulticastConfig().setEnabled(false);
		networkConfig.getJoin().getTcpIpConfig().setEnabled(false);
		networkConfig.getJoin().getAwsConfig().setEnabled(false);

		List<HazelcastDiscoveryConfigurer> matchingDiscoveryConfigurers = new ArrayList<>();
		if (discoveryConfigurers != null) {
			matchingDiscoveryConfigurers = discoveryConfigurers.stream()
					.filter(dc -> dc.supportedDiscoveryType() == hazelcastProperties.getDiscoveryType()).collect(Collectors.toList());
		}
		if (matchingDiscoveryConfigurers.size() != 1) {
			throw new IllegalStateException();
		}
		matchingDiscoveryConfigurers.get(0).configure(config);

		if (hazelcastProperties.getCaches() != null) {
			hazelcastProperties.getCaches().forEach(mapConfig -> config.addMapConfig(mapConfig));
		}

		config.setProperty(GroupProperty.ENABLE_JMX.getName(), "true");
		config.setProperty(GroupProperty.HEALTH_MONITORING_LEVEL.getName(), "OFF");
		config.setProperty(GroupProperty.PHONE_HOME_ENABLED.getName(), "false");
		config.setProperty(GroupProperty.LOGGING_TYPE.getName(), "slf4j");

		return config;
	}

	@ConditionalOnClass(HazelcastInstance.class)
	@Bean
	HazelcastPublicMetrics hazelcastPublicMetrics(HazelcastInstance hazelcastInstance) {
		return new HazelcastPublicMetrics(hazelcastInstance);
	}

	@ConditionalOnClass(SpringManagedContext.class)
	protected static class SpringManagedContextConfiguration {
		@Bean
		SpringManagedContext springManagedContext(Config hazelcastConfig) {
			SpringManagedContext managedContext = new SpringManagedContext();
			hazelcastConfig.setManagedContext(managedContext);

			return managedContext;
		}
	}

	@Configuration
	@ComponentScan("de.enterprise.spring.boot.application.starter.clustering.scheduling")
	@ConditionalOnBean(ScheduledAnnotationBeanPostProcessor.class)
	protected static class HazelcastSchedulingAutoConfiguration {

		@Bean
		public ScheduledTaskExecutionProtocol scheduledTaskExecutionProtocol(HazelcastInstance hazelcastInstance,
				HazelcastProperties hazelcastProperties, MeterRegistry meterRegistry) {
			return new ScheduledTaskExecutionProtocol(hazelcastInstance, hazelcastProperties.getLoggingProperties(), meterRegistry);
		}

		@Bean(destroyMethod = "destroy")
		public TaskScheduler taskScheduler(HazelcastInstance hazelcastInstance, MeterRegistry meterRegistry,
				ScheduledTaskExecutionProtocol scheduledTaskExecutionProtocol, TracingProperties tracingProperties) {
			HazelcastTaskScheduler taskScheduler = new HazelcastTaskScheduler(hazelcastInstance, meterRegistry,
					scheduledTaskExecutionProtocol,
					tracingProperties);
			taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
			taskScheduler.setAwaitTerminationSeconds(60);
			taskScheduler.setPoolSize(5);

			return taskScheduler;
		}

		@Bean(destroyMethod = "destroy")
		public TaskExecutor taskExecutor() {
			ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
			taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
			taskExecutor.setAwaitTerminationSeconds(60);
			taskExecutor.setCorePoolSize(10);
			taskExecutor.setMaxPoolSize(20);
			taskExecutor.setTaskDecorator(new MdcTaskDecorator());
			return taskExecutor;
		}
	}

	@Configuration
	@ConditionalOnClass({ HazelcastInstance.class, HealthIndicator.class })
	@AutoConfigureAfter({ HealthContributorAutoConfiguration.class })
	@ConditionalOnProperty(value = "management.health.hazelcast.enabled", matchIfMissing = true)
	protected static class HazelcastHealthAutoConfiguration {

		@Bean
		public HazelcastHealthIndicator hazelcastHealthIndicator(HazelcastInstance hazelcastInstance,
				@Autowired(required = false) ScheduledTaskExecutionProtocol scheduledTaskExecutionProtocol) {
			return new HazelcastHealthIndicator(hazelcastInstance, scheduledTaskExecutionProtocol);
		}

		@ConfigurationProperties("management.health.hazelcast")
		@Validated
		public static class Health {
			/**
			 * Flag to inidicate that the hystrix health indicator should be installed.
			 */
			boolean enabled;

			public boolean isEnabled() {
				return this.enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}
		}
	}
}
