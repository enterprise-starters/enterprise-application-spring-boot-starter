package de.enterprise.spring.boot.application.starter.clustering.discovery;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hazelcast.config.Config;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.spi.properties.GroupProperty;

import de.enterprise.spring.boot.application.starter.clustering.HazelcastProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for hazelcast discovery via TCP.
 *
 * @author Malte Geßner, Jonas Keßler
 */
@Slf4j
@Service
public class TcpHazelcastDiscoveryConfigurer implements HazelcastDiscoveryConfigurer {

	@Autowired
	private HazelcastProperties hazelcastProperties;

	@Override
	public HazelcastDiscoveryType supportedDiscoveryType() {
		return HazelcastDiscoveryType.Tcp;
	}

	@Override
	public void configure(Config hazelcastConfig) {
		NetworkConfig networkConfig = hazelcastConfig.getNetworkConfig();
		TcpIpConfig tcpIpConfig = new TcpIpConfig();
		tcpIpConfig.setEnabled(true);
		tcpIpConfig.setMembers(Arrays.asList(this.hazelcastProperties.getTcpDiscoveryConfig().getMembers()));

		networkConfig.getJoin().setTcpIpConfig(tcpIpConfig);

		if (this.hazelcastProperties.getTcpDiscoveryConfig().getPort() != null) {
			log.debug("[{}]: Overriding Port with '{}'", hazelcastConfig.getInstanceName(),
					this.hazelcastProperties.getTcpDiscoveryConfig().getPort());
			networkConfig.setPort(this.hazelcastProperties.getTcpDiscoveryConfig().getPort().intValue());
		}

		if (this.hazelcastProperties.getTcpDiscoveryConfig().getPortCount() != null) {
			log.debug("[{}]: Overriding PortCount with '{}'", hazelcastConfig.getInstanceName(),
					this.hazelcastProperties.getTcpDiscoveryConfig().getPortCount());
			networkConfig.setPortCount(this.hazelcastProperties.getTcpDiscoveryConfig().getPortCount());
		}

		// Falls portautoincrement aktiv ist, setze die Anzahl der Ports, auf
		// denen beim Startup nach Clustern mit der gleichen Gruppe gesucht
		// wird.
		if (networkConfig.isPortAutoIncrement()) {
			int ports = networkConfig.getPortCount();
			if (this.hazelcastProperties.getTcpDiscoveryConfig().getJoinPortTryCount() != null) {
				ports = this.hazelcastProperties.getTcpDiscoveryConfig().getJoinPortTryCount().intValue();
			}
			hazelcastConfig.setProperty(GroupProperty.TCP_JOIN_PORT_TRY_COUNT.getName(), String.valueOf(ports));
			log.debug("[{}]: Overriding joinPortTryCount with '{}'", hazelcastConfig.getInstanceName(), ports);
		}

	}

}
