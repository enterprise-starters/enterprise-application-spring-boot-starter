package de.enterprise.starters.kubernetes.clustering.hazelcast;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

/**
 * Hazelcast instance configuration properties.
 *
 * @author Malte Geßner (Acando GmbH)
 *
 */
@ConfigurationProperties(prefix = "enterprise-application.hazelcast.kubernetes-discovery-config", ignoreUnknownFields = false)
@Validated
@Getter
@Setter
public class HazelcastKubernetesProperties {

	private boolean useDns;

	private String serviceName;

	private boolean useLabel;
	private String labelName;
	private String labelValue;

	@NotNull
	@Min(1)
	private Integer dnsTimeoutInSeconds = 10;
}
