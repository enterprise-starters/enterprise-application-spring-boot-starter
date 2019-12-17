package de.enterprise.starters.aws.ecs.clustering.hazelcast.discovery.spi.discovery.aws;

/**
 *
 * @author Malte Geßner
 *
 */
public class PublicHazelcastAddressDiscoveryException extends AmazonECSDiscoveryException {
	private static final long serialVersionUID = 201806180800L;

	public PublicHazelcastAddressDiscoveryException(String message) {
		super(message);
	}

	public PublicHazelcastAddressDiscoveryException(String message, Throwable cause) {
		super(message, cause);
	}
}
