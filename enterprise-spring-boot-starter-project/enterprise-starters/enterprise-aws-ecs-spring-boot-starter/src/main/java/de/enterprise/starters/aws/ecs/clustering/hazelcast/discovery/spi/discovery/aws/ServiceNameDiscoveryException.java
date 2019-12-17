package de.enterprise.starters.aws.ecs.clustering.hazelcast.discovery.spi.discovery.aws;

/**
 *
 * @author Malte Geßner
 *
 */
public class ServiceNameDiscoveryException extends AmazonECSDiscoveryException {
	private static final long serialVersionUID = 201806180800L;

	public ServiceNameDiscoveryException(Throwable cause) {
		super("Service name discovery failed", cause);
	}
}
