/*
 * Copyright 2002-2012 the original author or authors.
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

package de.enterprise.spring.boot.application.starter.clustering.scheduling;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.DelegatingErrorHandlingRunnable;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.util.ErrorHandler;

import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.ILock;

import lombok.extern.slf4j.Slf4j;

/**
 * Copy of {@link org.springframework.scheduling.concurrent.ReschedulingRunnable} with extension for hazelcast cluster sync mechanismn. Each
 * task execution was only processed on one cluster node/member.
 *
 * @author Malte Gessner
 */
@Slf4j
public class HazelcastReschedulingRunnable extends DelegatingErrorHandlingRunnable implements ScheduledFuture<Object> {

	private final Trigger trigger;

	private final SimpleTriggerContext triggerContext = new SimpleTriggerContext();

	private final ScheduledExecutorService executor;
	private final String taskName;

	private final IAtomicLong lastExecutionTimeInMillis;
	private ScheduledTaskExecutionProtocol scheduledTaskExecutionProtocol;
	private final ILock taskLock;

	private ScheduledFuture<?> currentFuture;

	private Date scheduledExecutionTime;

	private final Object triggerContextMonitor = new Object();

	public HazelcastReschedulingRunnable(Runnable delegate, String taskName, Trigger trigger, ScheduledExecutorService executor,
			IAtomicLong lastExecutionTimeInMillis, ScheduledTaskExecutionProtocol scheduledTaskExecutionProtocol, ILock taskLock,

			ErrorHandler errorHandler) {
		super(delegate, errorHandler);
		this.trigger = trigger;
		this.executor = executor;
		this.lastExecutionTimeInMillis = lastExecutionTimeInMillis;
		this.scheduledTaskExecutionProtocol = scheduledTaskExecutionProtocol;
		this.taskLock = taskLock;
		this.taskName = taskName;
	}

	public ScheduledFuture<?> schedule() {
		synchronized (this.triggerContextMonitor) {
			this.scheduledExecutionTime = this.trigger.nextExecutionTime(this.triggerContext);
			if (this.scheduledExecutionTime == null) {
				return null;
			}

			this.lastExecutionTimeInMillis.set(this.scheduledExecutionTime.getTime());

			long initialDelay = this.scheduledExecutionTime.getTime() - System.currentTimeMillis();
			this.currentFuture = this.executor.schedule(this, initialDelay, TimeUnit.MILLISECONDS);
			return this;
		}
	}

	@Override
	public void run() {
		Date actualExecutionTime = new Date();
		try {
			log.debug("try execute task for executionTime <{}>, actualTime <{}>", this.scheduledExecutionTime.getTime(),
					actualExecutionTime.getTime());
			if (this.taskLock.tryLock(50, TimeUnit.MILLISECONDS)) {
				log.debug("lock <{}>", this.taskLock.getName());
				if (this.lastExecutionTimeInMillis.compareAndSet(this.scheduledExecutionTime.getTime(),
						actualExecutionTime.getTime() + 1)) {
					this.taskLock.unlock();
					log.debug("unlocked <{}>", this.taskLock.getName());
					log.debug("execute task for executionTime <{}>, actualTime <{}>", this.scheduledExecutionTime.getTime(),
							actualExecutionTime.getTime());
					try {
						this.scheduledTaskExecutionProtocol.startExecution(this.taskName);
						LocalDateTime executionTime = LocalDateTime.now();
						super.run();
						this.scheduledTaskExecutionProtocol.finishedExecution(this.taskName, executionTime);
					} catch (Exception e) {
						this.scheduledTaskExecutionProtocol.failedExecution(this.taskName);
					}
				} else {
					this.taskLock.unlock();
					log.debug("unlocked <{}>", this.taskLock.getName());
				}
			} else {
				log.debug("try lock failed <{}>", this.taskLock.getName());
			}
		} catch (InterruptedException e) {
			log.error("lock interrupted", e);
		}

		Date completionTime = new Date();
		synchronized (this.triggerContextMonitor) {
			this.triggerContext.update(this.scheduledExecutionTime, actualExecutionTime, completionTime);
			if (!this.currentFuture.isCancelled()) {
				this.schedule();
			}
		}
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		synchronized (this.triggerContextMonitor) {
			return this.currentFuture.cancel(mayInterruptIfRunning);
		}
	}

	@Override
	public boolean isCancelled() {
		synchronized (this.triggerContextMonitor) {
			return this.currentFuture.isCancelled();
		}
	}

	@Override
	public boolean isDone() {
		synchronized (this.triggerContextMonitor) {
			return this.currentFuture.isDone();
		}
	}

	@Override
	public Object get() throws InterruptedException, ExecutionException {
		ScheduledFuture<?> curr;
		synchronized (this.triggerContextMonitor) {
			curr = this.currentFuture;
		}
		return curr.get();
	}

	@Override
	public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		ScheduledFuture<?> curr;
		synchronized (this.triggerContextMonitor) {
			curr = this.currentFuture;
		}
		return curr.get(timeout, unit);
	}

	@Override
	public long getDelay(TimeUnit unit) {
		ScheduledFuture<?> curr;
		synchronized (this.triggerContextMonitor) {
			curr = this.currentFuture;
		}
		return curr.getDelay(unit);
	}

	@Override
	public int compareTo(Delayed other) {
		if (this == other) {
			return 0;
		}
		long diff = this.getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS);
		return diff == 0 ? 0 : diff < 0 ? -1 : 1;
	}

	public String getTaskName() {
		return this.taskName;
	}
}
