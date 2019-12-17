package de.enterprise.starters.aws.ecs.clustering.hazelcast.discovery.spi.discovery.aws;

/**
 *
 * @author Malte Geßner
 *
 */
public class PublicHazelcastAddressNotFoundException extends PublicHazelcastAddressDiscoveryException {
	private static final long serialVersionUID = 201806180800L;

	public PublicHazelcastAddressNotFoundException() {
		super("Public Hazelcast address not found");
	}
}
