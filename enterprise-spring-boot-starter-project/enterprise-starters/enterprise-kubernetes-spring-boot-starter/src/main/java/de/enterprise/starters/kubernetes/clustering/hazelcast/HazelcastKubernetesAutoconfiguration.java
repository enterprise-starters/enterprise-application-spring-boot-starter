package de.enterprise.starters.kubernetes.clustering.hazelcast;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.HazelcastInstance;

import de.enterprise.spring.boot.application.starter.clustering.HazelcastAutoConfiguration;
import de.enterprise.spring.boot.application.starter.clustering.discovery.HazelcastDiscoveryConfigurer;
import de.enterprise.starters.kubernetes.clustering.hazelcast.discovery.KubernetesHazelcastDiscoveryConfigurer;

/**
 *
 * @author Jonas Keßler
 */
@Configuration
@ConditionalOnClass(HazelcastInstance.class)
@AutoConfigureBefore(HazelcastAutoConfiguration.class)
public class HazelcastKubernetesAutoconfiguration {

	@Bean
	public HazelcastDiscoveryConfigurer awsEcsHazelcastDiscoveryConfigurer() {
		return new KubernetesHazelcastDiscoveryConfigurer();
	}
}
