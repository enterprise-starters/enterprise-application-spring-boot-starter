package de.enterprise.starters.aws.ecs.clustering.hazelcast.discovery.spi.discovery.aws;

/**
 *
 * @author Malte Geßner
 *
 */
public class AmazonECSDiscoveryException extends Exception {

	private static final long serialVersionUID = 201806180800L;

	public AmazonECSDiscoveryException(String message) {
		super(message);
	}

	public AmazonECSDiscoveryException(String message, Throwable cause) {
		super(message, cause);
	}

}
