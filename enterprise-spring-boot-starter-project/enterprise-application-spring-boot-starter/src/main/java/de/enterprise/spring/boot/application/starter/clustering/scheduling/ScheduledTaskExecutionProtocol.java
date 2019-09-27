package de.enterprise.spring.boot.application.starter.clustering.scheduling;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Malte Gessner
 */
@Slf4j
public class ScheduledTaskExecutionProtocol {

	private static final String TAG_NAME_TASK_NAME = "taskName";

	private static final String LAST_SUCCESSFULL_EXECUTION_HAZELCAST_PREFIX = "lastSuccessfullExecution-";

	private HazelcastInstance hazelcastInstance;
	private ScheduledTaskLoggingProperties loggingProperties;
	private MeterRegistry metricRegistry;
	private Map<String, TaskState> taskMap;

	@Autowired
	public ScheduledTaskExecutionProtocol(HazelcastInstance hazelcastInstance, ScheduledTaskLoggingProperties loggingProperties,
			MeterRegistry metricRegistry) {
		this.hazelcastInstance = hazelcastInstance;
		this.loggingProperties = loggingProperties;
		this.metricRegistry = metricRegistry;
		this.taskMap = new HashMap<>();
	}

	public String getTaskName(String className, String methodName) {
		return className + "-" + methodName;
	}

	private String getLastExecutionTaskName(String taskName) {
		return LAST_SUCCESSFULL_EXECUTION_HAZELCAST_PREFIX + taskName;
	}

	public void registerTask(String taskName, Trigger trigger) {
		String cronExpression = null;
		if (trigger instanceof CronTrigger) {
			cronExpression = ((CronTrigger) trigger).getExpression();
		}
		this.taskMap.put(taskName, new TaskState(cronExpression));
	}

	public void startExecution(String taskName) {
		this.metricRegistry.counter("hazelcast.cluster.scheduling.startExecution", Tags.of(TAG_NAME_TASK_NAME, taskName)).increment();

		if (this.shouldLog(taskName)) {
			log.info("Start CronJob job={}", taskName);
		}
	}

	public void finishedExecution(String taskName, LocalDateTime executionTime) {
		if (this.shouldLog(taskName)) {
			log.info("Finished CronJob job={}", taskName);
		}

		this.metricRegistry.counter("hazelcast.cluster.scheduling.finishedExecution", Tags.of(TAG_NAME_TASK_NAME, taskName)).increment();
		this.updateExecutionTimeOfTask(taskName, executionTime);
	}

	public void failedExecution(String taskName) {
		if (this.shouldLog(taskName)) {
			log.info("Failed CronJob job={}", taskName);
		}

		this.metricRegistry.counter("hazelcast.cluster.scheduling.failedExecution", Tags.of(TAG_NAME_TASK_NAME, taskName)).increment();
	}

	private boolean shouldLog(String taskName) {
		return this.loggingProperties.isEnabled() && !ArrayUtils.contains(this.loggingProperties.getIgnoredTasks(), taskName);
	}

	private void updateExecutionTimeOfTask(String taskName, LocalDateTime time) {
		TaskState taskState = this.taskMap.get(taskName);
		if (taskState != null) {
			taskState.setLocalLastSuccessfullExecutionTime(time);
		}

		this.hazelcastInstance.getAtomicLong(this.getLastExecutionTaskName(taskName))
				.set(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
	}

	public Map<String, TaskState> getTaskProtocol() {
		for (String key : this.taskMap.keySet()) {
			IAtomicLong lastExecutionTimeInMillis = this.hazelcastInstance.getAtomicLong(LAST_SUCCESSFULL_EXECUTION_HAZELCAST_PREFIX + key);
			long millis = lastExecutionTimeInMillis.get();
			if (millis == 0) {
				continue;
			}
			Instant epochMilli = Instant.ofEpochMilli(millis);
			LocalDateTime clusterLastExecutionTime = LocalDateTime.ofInstant(epochMilli, ZoneId.systemDefault());

			this.taskMap.get(key).setClusterLastSuccessfullExecutionTime(clusterLastExecutionTime);
		}

		return this.taskMap;
	}

	@Setter
	@Getter
	private class TaskState {
		private String cronExpression;
		private LocalDateTime localLastSuccessfullExecutionTime;
		private LocalDateTime clusterLastSuccessfullExecutionTime;

		TaskState(String cronExpression) {
			this.cronExpression = cronExpression;
		}
	}
}
