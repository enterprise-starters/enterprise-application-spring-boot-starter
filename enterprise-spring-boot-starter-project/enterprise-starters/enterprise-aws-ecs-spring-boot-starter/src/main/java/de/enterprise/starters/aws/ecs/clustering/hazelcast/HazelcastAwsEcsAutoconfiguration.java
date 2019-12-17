package de.enterprise.starters.aws.ecs.clustering.hazelcast;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.HazelcastInstance;

import de.enterprise.spring.boot.application.starter.clustering.HazelcastAutoConfiguration;
import de.enterprise.spring.boot.application.starter.clustering.discovery.HazelcastDiscoveryConfigurer;
import de.enterprise.starters.aws.ecs.clustering.hazelcast.discovery.AwsEcsHazelcastDiscoveryConfigurer;

/**
 *
 * @author Jonas Keßler
 */
@Configuration
@ConditionalOnClass(HazelcastInstance.class)
@AutoConfigureBefore(HazelcastAutoConfiguration.class)
public class HazelcastAwsEcsAutoconfiguration {

	@Bean
	public HazelcastDiscoveryConfigurer awsEcsHazelcastDiscoveryConfigurer() {
		return new AwsEcsHazelcastDiscoveryConfigurer();
	}
}
