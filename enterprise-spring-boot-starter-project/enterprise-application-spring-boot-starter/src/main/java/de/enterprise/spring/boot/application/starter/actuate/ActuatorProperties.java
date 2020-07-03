package de.enterprise.spring.boot.application.starter.actuate;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Malte Geßner
 *
 */
@ConfigurationProperties(prefix = "enterprise-application.actuator")
@Getter
@Setter
@Validated
public class ActuatorProperties {

	/**
	 * If no security configuration is defined in the application, actuator endpoints are secured through the
	 * {@link ActuatorSecurityConfiguration}.
	 */
	private Boolean endpointSecurityEnabled;
	private String instanceCommonTagValue;
	private String versionCommonTagValue;
}
