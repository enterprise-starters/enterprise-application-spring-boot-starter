package de.enterprise.starters.aws.ecs.clustering.hazelcast.discovery.spi.discovery.aws;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ecs.AmazonECS;
import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryStrategyFactory;

/**
 *
 * @author Malte Geßner
 *
 */
public class AmazonECSDiscoveryStrategyFactory implements DiscoveryStrategyFactory {

	private final AmazonECS ecsClient;
	private final AmazonEC2 ec2Client;
	private final int containerPort;

	public AmazonECSDiscoveryStrategyFactory(AmazonECS ecsClient, AmazonEC2 ec2Client, int containerPort) {
		this.ecsClient = ecsClient;
		this.ec2Client = ec2Client;
		this.containerPort = containerPort;
	}

	@Override
	public Class<? extends DiscoveryStrategy> getDiscoveryStrategyType() {
		return AmazonECSDiscoveryStrategy.class;
	}

	@Override
	public Collection<PropertyDefinition> getConfigurationProperties() {
		return Collections.unmodifiableCollection(Collections.emptyList());
	}

	@SuppressWarnings("rawtypes")
	@Override
	public DiscoveryStrategy newDiscoveryStrategy(DiscoveryNode discoveryNode,
			ILogger logger,
			Map<String, Comparable> properties) {

		return new AmazonECSDiscoveryStrategy(logger, properties, this.ecsClient, this.ec2Client, this.containerPort);
	}
}
