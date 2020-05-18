package de.enterprise.spring.boot.application.starter.clustering.discovery;

import com.hazelcast.config.Config;

/**
 *
 * @author Jonas Keßler
 */
public interface HazelcastDiscoveryConfigurer {

	HazelcastDiscoveryType supportedDiscoveryType();

	void configure(Config hazelcastConfig);

}
