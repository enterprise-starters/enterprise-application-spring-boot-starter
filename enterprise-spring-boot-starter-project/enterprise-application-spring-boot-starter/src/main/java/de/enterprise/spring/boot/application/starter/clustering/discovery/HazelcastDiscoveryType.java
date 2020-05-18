package de.enterprise.spring.boot.application.starter.clustering.discovery;

/**
 * Possible hazelcast discovery types, with prepared configuration.
 *
 * Concrete configuration (class implementing {@link HazelcastDiscoveryConfigurer}) can be in another starter, e.g. the discovery
 * configuration for AWS-ECS can be found in the package enterprise-aws-spring-boot-starter.
 *
 * @author Malte Geßner
 *
 */
public enum HazelcastDiscoveryType {
	Tcp, AwsEcs, Kubernetes
}
