package de.enterprise.starters.kubernetes.clustering.hazelcast;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.HazelcastInstance;

import de.enterprise.spring.boot.application.starter.clustering.discovery.HazelcastDiscoveryConfigurer;
import de.enterprise.starters.kubernetes.clustering.hazelcast.discovery.KubernetesHazelcastDiscoveryConfigurer;

/**
 *
 * @author Jonas Keßler
 */
@Configuration
@ConditionalOnClass(HazelcastInstance.class)
@EnableConfigurationProperties({ HazelcastKubernetesProperties.class })
@AutoConfigureBefore(HazelcastAutoConfiguration.class)
public class HazelcastKubernetesAutoConfiguration {

	@ConditionalOnProperty(name = "enterprise-application.hazelcast.discovery-type", havingValue = "kubernetes")
	@Bean
	public HazelcastDiscoveryConfigurer kubernetesHazelcastDiscoveryConfigurer() {
		return new KubernetesHazelcastDiscoveryConfigurer();
	}
}
