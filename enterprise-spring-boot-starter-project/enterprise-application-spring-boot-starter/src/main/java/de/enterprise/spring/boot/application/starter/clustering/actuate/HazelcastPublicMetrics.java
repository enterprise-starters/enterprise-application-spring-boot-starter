package de.enterprise.spring.boot.application.starter.clustering.actuate;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.ICountDownLatch;
import com.hazelcast.core.IList;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ISet;
import com.hazelcast.core.ITopic;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Collect metrics from hazelcast.
 *
 * @author Malte Gessner
 */
public class HazelcastPublicMetrics implements MeterBinder {

	private static final String TAG_NAME_OBJECT_NAME = "name";
	private final HazelcastInstance hazelcastInstance;

	public HazelcastPublicMetrics(HazelcastInstance hazelcastInstance) {
		this.hazelcastInstance = hazelcastInstance;
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		this.addClusterServiceMetrics(registry);
		this.addDistributedObjectMetrics(registry);
	}

	/**
	 * Add cluster metrics.
	 *
	 * @param registry
	 *            meter registry
	 */
	protected void addClusterServiceMetrics(MeterRegistry registry) {
		Cluster cluster = this.hazelcastInstance.getCluster();
		Gauge.builder("hazelcast.cluster.size", cluster, (clusterObject) -> clusterObject.getMembers().size()).register(registry);
		Gauge.builder("hazelcast.cluster.state", cluster, (clusterObject) -> clusterObject.getClusterState().ordinal()).register(registry);
	}

	/**
	 * Add map metrics.
	 *
	 * @param registry
	 *            meter registry
	 */
	protected void addDistributedObjectMetrics(MeterRegistry registry) {
		for (DistributedObject distributedObject : this.hazelcastInstance.getDistributedObjects()) {
			if (distributedObject instanceof IMap) {
				this.addDistributedObjectMetrics(distributedObject.getName(), (IMap<?, ?>) distributedObject, registry);
			} else if (distributedObject instanceof IQueue) {
				this.addDistributedObjectMetrics(distributedObject.getName(), (IQueue<?>) distributedObject, registry);
			} else if (distributedObject instanceof IAtomicLong) {
				this.addDistributedObjectMetrics(distributedObject.getName(), (IAtomicLong) distributedObject, registry);
			} else if (distributedObject instanceof ICountDownLatch) {
				this.addDistributedObjectMetrics(distributedObject.getName(), (ICountDownLatch) distributedObject, registry);
			} else if (distributedObject instanceof ITopic) {
				this.addDistributedObjectMetrics(distributedObject.getName(), (ITopic<?>) distributedObject, registry);
			} else if (distributedObject instanceof ISet) {
				this.addDistributedObjectMetrics(distributedObject.getName(), (ISet<?>) distributedObject, registry);
			} else if (distributedObject instanceof IList) {
				this.addDistributedObjectMetrics(distributedObject.getName(), (IList<?>) distributedObject, registry);
			}
		}
	}

	protected void addDistributedObjectMetrics(String name, IAtomicLong atomicLong, MeterRegistry registry) {
		registry.gauge("hazelcast.atomiclong.currentValue", Tags.of(TAG_NAME_OBJECT_NAME, name), atomicLong.get());
	}

	protected void addDistributedObjectMetrics(String name, ICountDownLatch countDownLatch, MeterRegistry registry) {
		registry.gauge("hazelcast.countdownlatch.currentValue", Tags.of(TAG_NAME_OBJECT_NAME, name), countDownLatch.getCount());
	}

	protected void addDistributedObjectMetrics(String name, ITopic<?> topic, MeterRegistry registry) {
		registry.gauge("hazelcast.topic.publishOperationCount", Tags.of(TAG_NAME_OBJECT_NAME, name),
				topic.getLocalTopicStats().getPublishOperationCount());
		registry.gauge("hazelcast.topic.receiveOperationCount", Tags.of(TAG_NAME_OBJECT_NAME, name),
				topic.getLocalTopicStats().getReceiveOperationCount());
	}

	protected void addDistributedObjectMetrics(String name, ISet<?> set, MeterRegistry registry) {
		registry.gauge("hazelcast.set.size", Tags.of(TAG_NAME_OBJECT_NAME, name), set.size());
	}

	protected void addDistributedObjectMetrics(String name, IList<?> list, MeterRegistry registry) {
		registry.gauge("hazelcast.list.size", Tags.of(TAG_NAME_OBJECT_NAME, name), list.size());
	}

	/**
	 * Add map metrics.
	 *
	 * @param name
	 *            Tag name
	 * @param map
	 *            map
	 * @param registry
	 *            meter registry
	 */
	protected void addDistributedObjectMetrics(String name, IMap<?, ?> map, MeterRegistry registry) {
		// Map Memory Data Table (hazelcast memcenter)
		registry.gauge("hazelcast.map.ownedEntryCount", Tags.of(TAG_NAME_OBJECT_NAME, name), map.getLocalMapStats().getOwnedEntryCount());
		registry.gauge("hazelcast.map.ownedEntryMemoryCost", Tags.of(TAG_NAME_OBJECT_NAME, name),
				map.getLocalMapStats().getOwnedEntryMemoryCost());
		registry.gauge("hazelcast.map.backupEntryCount", Tags.of(TAG_NAME_OBJECT_NAME, name), map.getLocalMapStats().getBackupEntryCount());
		registry.gauge("hazelcast.map.backupEntryMemoryCost", Tags.of(TAG_NAME_OBJECT_NAME, name),
				map.getLocalMapStats().getBackupEntryMemoryCost());
		registry.gauge("hazelcast.map.eventOperationCount", Tags.of(TAG_NAME_OBJECT_NAME, name),
				map.getLocalMapStats().getEventOperationCount());
		registry.gauge("hazelcast.map.hits", Tags.of(TAG_NAME_OBJECT_NAME, name), map.getLocalMapStats().getHits());
		registry.gauge("hazelcast.map.lockedEntryCount", Tags.of(TAG_NAME_OBJECT_NAME, name), map.getLocalMapStats().getLockedEntryCount());
		registry.gauge("hazelcast.map.dirtyEntryCount", Tags.of(TAG_NAME_OBJECT_NAME, name), map.getLocalMapStats().getDirtyEntryCount());

		// Map Throughput Data Table (hazelcast memcenter)
		registry.gauge("hazelcast.map.putOperationCount", Tags.of(TAG_NAME_OBJECT_NAME, name),
				map.getLocalMapStats().getPutOperationCount());
		registry.gauge("hazelcast.map.getOperationCount", Tags.of(TAG_NAME_OBJECT_NAME, name),
				map.getLocalMapStats().getGetOperationCount());
		registry.gauge("hazelcast.map.removeOperationCount, Tags.of(\"name\", name)", map.getLocalMapStats().getRemoveOperationCount());
		registry.gauge("hazelcast.map.putLatencyMax", Tags.of(TAG_NAME_OBJECT_NAME, name), map.getLocalMapStats().getMaxPutLatency());

		if (map.getLocalMapStats().getPutOperationCount() > 0) {
			registry.gauge("hazelcast.map.putLatencyAvg", Tags.of(TAG_NAME_OBJECT_NAME, name),
					map.getLocalMapStats().getTotalPutLatency() / map.getLocalMapStats().getPutOperationCount());
		}

		if (map.getLocalMapStats().getGetOperationCount() > 0) {
			registry.gauge("hazelcast.map.getLatencyAvg", Tags.of(TAG_NAME_OBJECT_NAME, name),
					map.getLocalMapStats().getTotalGetLatency() / map.getLocalMapStats().getGetOperationCount());
		}

		if (map.getLocalMapStats().getRemoveOperationCount() > 0) {
			registry.gauge("hazelcast.map.removeLatencyAvg", Tags.of(TAG_NAME_OBJECT_NAME, name),
					map.getLocalMapStats().getTotalRemoveLatency() / map.getLocalMapStats().getRemoveOperationCount());
		}

		registry.gauge("hazelcast.map." + name + ".putLatencyMax", map.getLocalMapStats().getMaxPutLatency());
		registry.gauge("hazelcast.map." + name + ".getLatencyMax", map.getLocalMapStats().getMaxGetLatency());
		registry.gauge("hazelcast.map." + name + ".removeLatencyMax", map.getLocalMapStats().getMaxRemoveLatency());

		// Memory (hazelcast memcenter)
		registry.gauge("hazelcast.map." + name + ".heapCost", map.getLocalMapStats().getHeapCost());

		// Near Cache Data Table (hazelcast memcenter)
		if (map.getLocalMapStats().getNearCacheStats() != null) {
			registry.gauge("hazelcast.map." + name + ".nearcacheHits", map.getLocalMapStats().getNearCacheStats().getOwnedEntryCount());
			registry.gauge("hazelcast.map." + name + ".nearcacheHits",
					map.getLocalMapStats().getNearCacheStats().getOwnedEntryMemoryCost());
			registry.gauge("hazelcast.map." + name + ".nearcacheHits", map.getLocalMapStats().getNearCacheStats().getHits());
			registry.gauge("hazelcast.map." + name + ".nearcacheMisses", map.getLocalMapStats().getNearCacheStats().getMisses());
			registry.gauge("hazelcast.map." + name + ".nearcacheRatio", map.getLocalMapStats().getNearCacheStats().getRatio());
		}
	}

	/**
	 * Add queue metrics.
	 *
	 * @param name
	 *            queue name
	 * @param queue
	 *            queue object
	 * @param registry
	 *            meter registry
	 */
	protected void addDistributedObjectMetrics(String name, IQueue<?> queue, MeterRegistry registry) {
		registry.gauge("hazelcast.queue." + name + ".minAge", queue.getLocalQueueStats().getMinAge());
		registry.gauge("hazelcast.queue." + name + ".maxAge", queue.getLocalQueueStats().getMaxAge());
		registry.gauge("hazelcast.queue." + name + ".avgAge", queue.getLocalQueueStats().getAvgAge());
		registry.gauge("hazelcast.queue." + name + ".ownedItemCount", queue.getLocalQueueStats().getOwnedItemCount());
		registry.gauge("hazelcast.queue." + name + ".backupItemCount", queue.getLocalQueueStats().getBackupItemCount());

		registry.gauge("hazelcast.queue." + name + ".operationOfferCount", queue.getLocalQueueStats().getOfferOperationCount());
		registry.gauge("hazelcast.queue." + name + ".operationRejectedOfferCount",
				queue.getLocalQueueStats().getRejectedOfferOperationCount());
		registry.gauge("hazelcast.queue." + name + ".operationPollCount", queue.getLocalQueueStats().getPollOperationCount());
		registry.gauge("hazelcast.queue." + name + ".otherOperationsCount", queue.getLocalQueueStats().getOtherOperationsCount());
		registry.gauge("hazelcast.queue." + name + ".operationEventCount", queue.getLocalQueueStats().getEventOperationCount());
	}
}
