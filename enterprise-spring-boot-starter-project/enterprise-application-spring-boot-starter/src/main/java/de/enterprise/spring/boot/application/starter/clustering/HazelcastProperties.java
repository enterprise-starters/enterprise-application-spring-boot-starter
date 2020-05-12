package de.enterprise.spring.boot.application.starter.clustering;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.hazelcast.config.MapConfig;

import de.enterprise.spring.boot.application.starter.clustering.discovery.HazelcastDiscoveryType;
import de.enterprise.spring.boot.application.starter.clustering.scheduling.ScheduledTaskLoggingProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Hazelcast instance configuration properties.
 *
 * @author Malte Geßner
 *
 */
@ConfigurationProperties(prefix = "enterprise-application.hazelcast", ignoreUnknownFields = false)
@Validated
@Getter
@Setter
public class HazelcastProperties {

	@NotNull
	private String groupName;
	@NotNull
	private HazelcastDiscoveryType discoveryType = HazelcastDiscoveryType.Tcp;
	@Valid
	private TcpDiscoveryConfig tcpDiscoveryConfig;
	@Valid
	private ScheduledTaskLoggingProperties loggingProperties = new ScheduledTaskLoggingProperties();
	@Valid
	private List<MapConfig> caches;

	/**
	 * Hazelcast config values for tcp node discovery.
	 *
	 * @author Malte Geßner
	 *
	 */
	@Getter
	@Setter
	@Validated
	public static class TcpDiscoveryConfig {
		@NotNull
		private String[] members;
		@NotNull
		private Integer port;
		@Min(1)
		private Integer portCount = 10;
		private Integer joinPortTryCount;
	}
}
