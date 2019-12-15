package de.enterprise.starters.kubernetes.clustering.hazelcast.discovery;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.kubernetes.HazelcastKubernetesDiscoveryStrategyFactory;
import com.hazelcast.kubernetes.KubernetesProperties;
import com.hazelcast.spi.properties.GroupProperty;

import de.enterprise.spring.boot.application.starter.clustering.HazelcastProperties;
import de.enterprise.spring.boot.application.starter.clustering.discovery.HazelcastDiscoveryConfigurer;
import de.enterprise.spring.boot.application.starter.clustering.discovery.HazelcastDiscoveryType;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Jonas Keßler
 */
@Slf4j
public class KubernetesHazelcastDiscoveryConfigurer implements HazelcastDiscoveryConfigurer {

	@Autowired
	private HazelcastProperties hazelcastProperties;

	@Override
	public HazelcastDiscoveryType supportedDiscoveryType() {
		return HazelcastDiscoveryType.Kubernetes;
	}

	@Override
	public void configure(Config config) {
		log.info("Cofiguring hazelcast for kubernetes");

		@SuppressWarnings("rawtypes")
		Map<String, Comparable> properties = new HashMap<>();
		if (this.hazelcastProperties.getKubernetesDiscoveryConfig().isUseDns()) {
			properties.put(KubernetesProperties.SERVICE_DNS.key(),
					this.hazelcastProperties.getKubernetesDiscoveryConfig().getServiceName());
			properties.put(KubernetesProperties.SERVICE_DNS_TIMEOUT.key(),
					this.hazelcastProperties.getKubernetesDiscoveryConfig().getDnsTimeoutInSeconds());
		} else {
			properties.put(KubernetesProperties.SERVICE_NAME.key(),
					this.hazelcastProperties.getKubernetesDiscoveryConfig().getServiceName());
		}

		DiscoveryStrategyConfig discoveryStrategyConfig = new DiscoveryStrategyConfig(new HazelcastKubernetesDiscoveryStrategyFactory(),
				properties);

		config.getNetworkConfig().getJoin().getDiscoveryConfig().addDiscoveryStrategyConfig(discoveryStrategyConfig);

		config.setProperty(GroupProperty.DISCOVERY_SPI_ENABLED.getName(), "true");
	}

}
