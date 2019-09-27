package de.enterprise.spring.boot.application.starter.clustering.scheduling;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;

import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.scheduling.support.TaskUtils;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.ILock;

import de.enterprise.spring.boot.application.starter.tracing.TracingProperties;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Hazelcast based task scheduler. Prevents the execution on many cluster node/members per task schedule date.
 *
 * Supports only the cron triggered task.
 *
 * @author Malte Gessner
 *
 */
public class HazelcastTaskScheduler extends ThreadPoolTaskScheduler {

	private static final long serialVersionUID = 201512160811L;

	private volatile HazelcastInstance hazelcastInstance;
	private volatile MeterRegistry metricRegistry;
	private ScheduledTaskExecutionProtocol scheduledTaskExecutionProtocol;
	private TracingProperties tracingProperties;

	public HazelcastTaskScheduler(HazelcastInstance hazelcastInstance, MeterRegistry metricRegistry,
			ScheduledTaskExecutionProtocol scheduledTaskExecutionProtocol, TracingProperties tracingProperties) {
		this.hazelcastInstance = hazelcastInstance;
		this.metricRegistry = metricRegistry;
		this.scheduledTaskExecutionProtocol = scheduledTaskExecutionProtocol;
		this.tracingProperties = tracingProperties;
	}

	@Override
	protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory,
			RejectedExecutionHandler rejectedExecutionHandler) {
		return new InstrumentedScheduledExecutorService(super.createExecutor(poolSize, threadFactory, rejectedExecutionHandler),
				this.metricRegistry, this.tracingProperties);
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {

		ScheduledMethodRunnable scheduledMethodRunnable = (ScheduledMethodRunnable) task;

		String className = scheduledMethodRunnable.getMethod().getDeclaringClass().getSimpleName();
		String methodName = scheduledMethodRunnable.getMethod().getName();
		String taskName = this.scheduledTaskExecutionProtocol.getTaskName(className, methodName);
		IAtomicLong lastExecutionTimeInMillis = this.hazelcastInstance.getAtomicLong(taskName);

		this.scheduledTaskExecutionProtocol.registerTask(taskName, trigger);

		ILock taskLock = this.hazelcastInstance.getLock(taskName);

		ScheduledExecutorService executor = this.getScheduledExecutor();
		try {
			return new HazelcastReschedulingRunnable(task, taskName, trigger, executor, lastExecutionTimeInMillis,
					this.scheduledTaskExecutionProtocol, taskLock, TaskUtils.getDefaultErrorHandler(false)).schedule();
		} catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}
}
