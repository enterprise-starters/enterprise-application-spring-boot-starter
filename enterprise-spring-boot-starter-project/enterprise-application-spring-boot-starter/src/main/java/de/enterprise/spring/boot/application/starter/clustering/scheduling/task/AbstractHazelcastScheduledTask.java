package de.enterprise.spring.boot.application.starter.clustering.scheduling.task;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

/**
 * Base class to transfer/persist data cluster save from task execution to task execution e.g. last execution time.
 *
 * @author Malte Gessner
 *
 */
public abstract class AbstractHazelcastScheduledTask {

	@Autowired
	private HazelcastInstance hazelcastInstance;

	public Object getTaskProperty(String name) {
		IMap<String, Object> concurrentMap = this.getMap();
		Object value = concurrentMap.get(name);
		return value;
	}

	public void setTaskProperty(String name, Serializable value) {
		IMap<String, Object> concurrentMap = this.getMap();
		concurrentMap.put(name, value);
	}

	private IMap<String, Object> getMap() {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		String callingClassName = stackTrace[3].getClassName();

		IMap<String, Object> concurrentMap = this.hazelcastInstance.<String, Object> getMap(callingClassName);

		return concurrentMap;
	}
}
