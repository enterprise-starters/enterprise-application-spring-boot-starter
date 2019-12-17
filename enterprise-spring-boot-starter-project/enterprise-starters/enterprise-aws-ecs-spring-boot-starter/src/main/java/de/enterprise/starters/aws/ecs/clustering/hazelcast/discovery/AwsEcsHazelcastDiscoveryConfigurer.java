package de.enterprise.starters.aws.ecs.clustering.hazelcast.discovery;

import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.spi.properties.GroupProperty;

import de.enterprise.spring.boot.application.starter.clustering.discovery.HazelcastDiscoveryConfigurer;
import de.enterprise.spring.boot.application.starter.clustering.discovery.HazelcastDiscoveryType;
import de.enterprise.starters.aws.ecs.clustering.hazelcast.discovery.spi.discovery.aws.AmazonECSDiscoveryException;
import de.enterprise.starters.aws.ecs.clustering.hazelcast.discovery.spi.discovery.aws.AmazonECSDiscoveryStrategyFactory;
import de.enterprise.starters.aws.ecs.clustering.hazelcast.discovery.spi.discovery.aws.AmazonECSDiscoveryUtils;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Jonas Keßler
 */
@Slf4j
public class AwsEcsHazelcastDiscoveryConfigurer implements HazelcastDiscoveryConfigurer {

	@Override
	public HazelcastDiscoveryType supportedDiscoveryType() {
		return HazelcastDiscoveryType.AwsEcs;
	}

	@Override
	public void configure(Config config) {
		NetworkConfig networkConfig = config.getNetworkConfig();
		JoinConfig joinConfig = networkConfig.getJoin();

		config.setProperty(GroupProperty.DISCOVERY_SPI_ENABLED.getName(), "true");

		AmazonECS amazonECS = AmazonECSClientBuilder.defaultClient();
		AmazonECSDiscoveryUtils amazonECSDiscoveryUtils = new AmazonECSDiscoveryUtils(amazonECS);
		try {
			networkConfig.setPublicAddress(
					amazonECSDiscoveryUtils.discoverPublicHazelcastAddress(networkConfig.getPort()));
		} catch (AmazonECSDiscoveryException e) {
			log.error("Failed to set public address on Hazelcast network config", e);
		}
		// No released version exist. Copied classes can be found in package
		// de.enterprise.starters.aws.ecs.clustering.hazelcast.discovery.spi.discovery.aws
		// https://github.com/commercehub-oss/hazelcast-discovery-amazon-ecs
		// TODO: check if still no released version exists
		joinConfig.getDiscoveryConfig().addDiscoveryStrategyConfig(
				new DiscoveryStrategyConfig(new AmazonECSDiscoveryStrategyFactory(
						amazonECS,
						AmazonEC2ClientBuilder.defaultClient(),
						networkConfig.getPort())));

	}

}
