/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package de.enterprise.spring.boot.application.starter.clustering.actuate;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;

import com.hazelcast.core.HazelcastInstance;

import de.enterprise.spring.boot.application.starter.clustering.scheduling.ScheduledTaskExecutionProtocol;

/**
 * A {@link HealthIndicator} implementation for Hazelcast instances.
 * <p>
 * This default implementation will not change the system state (e.g. <code>OK</code>) but includes all open hazelcast instances by name.
 *
 * @author Malte Gessner
 */
public class HazelcastHealthIndicator extends AbstractHealthIndicator {

	private HazelcastInstance hazelcastInstance;
	private ScheduledTaskExecutionProtocol scheduledTaskExecutionProtocol;

	public HazelcastHealthIndicator(HazelcastInstance hazelcastInstance, ScheduledTaskExecutionProtocol scheduledTaskExecutionProtocol) {
		this.hazelcastInstance = hazelcastInstance;
		this.scheduledTaskExecutionProtocol = scheduledTaskExecutionProtocol;
	}

	@Override
	protected void doHealthCheck(Builder builder) throws Exception {

		if (this.hazelcastInstance != null) {
			builder.up();

			builder.withDetail("cluster.size", this.hazelcastInstance.getCluster().getMembers().size());
			builder.withDetail("cluster.members", this.hazelcastInstance.getCluster().getMembers());
			if (this.scheduledTaskExecutionProtocol != null) {
				builder.withDetail("scheduling", this.scheduledTaskExecutionProtocol.getTaskProtocol());
			}
		} else {
			builder.outOfService();
		}
	}

}
