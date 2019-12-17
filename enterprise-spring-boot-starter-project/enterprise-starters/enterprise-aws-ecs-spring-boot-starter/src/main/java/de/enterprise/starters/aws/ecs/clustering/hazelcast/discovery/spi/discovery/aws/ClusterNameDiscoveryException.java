package de.enterprise.starters.aws.ecs.clustering.hazelcast.discovery.spi.discovery.aws;

/**
 *
 * @author Malte Geßner
 *
 */
public class ClusterNameDiscoveryException extends AmazonECSDiscoveryException {
	private static final long serialVersionUID = 201806180800L;

	public ClusterNameDiscoveryException(String message, Throwable cause) {
		super(message, cause);
	}
}
