package de.enterprise.spring.boot.application.starter.clustering.scheduling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.util.StopWatch;

import de.enterprise.spring.boot.application.starter.tracing.TracingProperties;
import de.enterprise.spring.boot.application.starter.tracing.TracingUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

/**
 * Instrumented Scheduled Executor Service inspired from com.codehale.metrics. Extended for fine grained metrics registration for each task.
 *
 * @author Malte Gessner
 */
public class InstrumentedScheduledExecutorService implements ScheduledExecutorService {
	private static final String TAG_NAME_TASK_NAME = "taskName";

	private final ScheduledExecutorService delegate;

	private final Map<String, AtomicInteger> submittedByTaskName;
	private final Map<String, AtomicInteger> runningByTaskName;
	private final Map<String, AtomicInteger> completedByTaskName;
	private final Map<String, Timer> durationByTaskName;
	private final Map<String, AtomicInteger> scheduledOnceByTaskName;
	private final Map<String, AtomicInteger> scheduledRepetitivelyByTaskName;
	private final Map<String, AtomicInteger> scheduledOverrunByTaskName;

	private final MeterRegistry registry;
	private final String baseName;
	private final TracingProperties tracingProperties;

	/**
	 * Wraps an {@link ScheduledExecutorService} uses an auto-generated default name.
	 *
	 * @param delegate
	 *            {@link ScheduledExecutorService} to wrap.
	 * @param registry
	 *            {@link MeterRegistry} that will contain the metrics.
	 * @param tracingProperties
	 *            {@link TracingProperties} that will configure tracing.
	 */
	public InstrumentedScheduledExecutorService(ScheduledExecutorService delegate, MeterRegistry registry,
			TracingProperties tracingProperties) {
		this(delegate, registry, "hazelcast.cluster.scheduling", tracingProperties);
	}

	/**
	 * Wraps an {@link ScheduledExecutorService} with an explicit name.
	 *
	 * @param delegate
	 *            {@link ScheduledExecutorService} to wrap.
	 * @param registry
	 *            {@link MeterRegistry} that will contain the metrics.
	 * @param name
	 *            name for this executor service.
	 * @param tracingProperties
	 *            {@link TracingProperties} that will configure tracing.
	 */
	public InstrumentedScheduledExecutorService(ScheduledExecutorService delegate, MeterRegistry registry, String name,
			TracingProperties tracingProperties) {
		this.delegate = delegate;

		this.registry = registry;
		this.baseName = name;
		this.tracingProperties = tracingProperties;

		this.submittedByTaskName = new HashMap<>();
		this.runningByTaskName = new HashMap<>();
		this.completedByTaskName = new HashMap<>();
		this.durationByTaskName = new HashMap<>();
		this.scheduledOnceByTaskName = new HashMap<>();
		this.scheduledRepetitivelyByTaskName = new HashMap<>();
		this.scheduledOverrunByTaskName = new HashMap<>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		scheduledOnce(command);
		return this.delegate.schedule(new InstrumentedRunnable(command), delay, unit);
	}

	protected String getTaskName(Runnable command) {
		if (command instanceof HazelcastReschedulingRunnable) {
			return ((HazelcastReschedulingRunnable) command).getTaskName();
		}

		return "default";
	}

	protected AtomicInteger getRunningCounter(Runnable command) {
		String taskName = this.getTaskName(command);
		if (!this.runningByTaskName.containsKey(taskName)) {
			this.runningByTaskName.put(taskName,
					this.registry.gauge(createMetricName("running"), Tags.of(TAG_NAME_TASK_NAME, taskName), new AtomicInteger(0)));
		}

		return this.runningByTaskName.get(taskName);
	}

	private void runningInc(Runnable command) {
		this.getRunningCounter(command).incrementAndGet();
	}

	private void runningDec(Runnable command) {
		this.getRunningCounter(command).decrementAndGet();
	}

	private void scheduledOnce(Runnable command) {
		String taskName = this.getTaskName(command);
		if (!this.scheduledOnceByTaskName.containsKey(taskName)) {
			this.scheduledOnceByTaskName.put(taskName,
					this.registry.gauge(createMetricName("scheduled.once"), Tags.of(TAG_NAME_TASK_NAME, taskName), new AtomicInteger(0)));
		}

		this.scheduledOnceByTaskName.get(taskName).incrementAndGet();
	}

	private void scheduledOverrun(Runnable command) {
		String taskName = this.getTaskName(command);
		if (!this.scheduledOverrunByTaskName.containsKey(taskName)) {
			this.scheduledOverrunByTaskName.put(taskName,
					this.registry.gauge(createMetricName("scheduled.overrun"), Tags.of(TAG_NAME_TASK_NAME, taskName),
							new AtomicInteger(0)));
		}

		this.scheduledOverrunByTaskName.get(taskName).incrementAndGet();
	}

	private void scheduledRepetitively(Runnable command) {
		String taskName = this.getTaskName(command);
		if (!this.scheduledRepetitivelyByTaskName.containsKey(taskName)) {
			this.scheduledRepetitivelyByTaskName.put(taskName,
					this.registry.gauge(createMetricName("scheduled.repetitively"), Tags.of(TAG_NAME_TASK_NAME, taskName),
							new AtomicInteger(0)));
		}

		this.scheduledRepetitivelyByTaskName.get(taskName).incrementAndGet();
	}

	private void submitted(Runnable command) {
		String taskName = this.getTaskName(command);
		if (!this.submittedByTaskName.containsKey(taskName)) {
			this.submittedByTaskName.put(taskName,
					this.registry.gauge(createMetricName("submitted"), Tags.of(TAG_NAME_TASK_NAME, taskName), new AtomicInteger(0)));
		}

		this.submittedByTaskName.get(taskName).incrementAndGet();
	}

	protected String createMetricName(String name) {
		return this.baseName + "." + name;
	}

	public void completed(Runnable command) {
		String taskName = this.getTaskName(command);
		if (!this.completedByTaskName.containsKey(taskName)) {
			this.completedByTaskName.put(taskName,
					this.registry.gauge(createMetricName("completed"), Tags.of(TAG_NAME_TASK_NAME, taskName), new AtomicInteger(0)));
		}

		this.completedByTaskName.get(taskName).incrementAndGet();
	}

	private Timer duration(Runnable command) {
		String taskName = this.getTaskName(command);
		if (!this.durationByTaskName.containsKey(taskName)) {
			this.durationByTaskName.put(taskName, this.registry.timer(createMetricName("duration"), Tags.of(TAG_NAME_TASK_NAME, taskName)));
		}

		return this.durationByTaskName.get(taskName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		return this.delegate.schedule(new InstrumentedCallable<>(callable), delay, unit);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		this.scheduledRepetitively(command);
		return this.delegate.scheduleAtFixedRate(new InstrumentedPeriodicRunnable(command, period, unit), initialDelay, period, unit);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		this.scheduledRepetitively(command);
		return this.delegate.scheduleAtFixedRate(new InstrumentedRunnable(command), initialDelay, delay, unit);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdown() {
		this.delegate.shutdown();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Runnable> shutdownNow() {
		return this.delegate.shutdownNow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isShutdown() {
		return this.delegate.isShutdown();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isTerminated() {
		return this.delegate.isTerminated();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return this.delegate.awaitTermination(timeout, unit);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return this.delegate.submit(new InstrumentedCallable<>(task));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		this.submitted(task);
		return this.delegate.submit(new InstrumentedRunnable(task), result);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Future<?> submit(Runnable task) {
		this.submitted(task);
		return this.delegate.submit(new InstrumentedRunnable(task));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		Collection<? extends Callable<T>> instrumented = this.instrument(tasks);
		return this.delegate.invokeAll(instrumented);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
		Collection<? extends Callable<T>> instrumented = this.instrument(tasks);
		return this.delegate.invokeAll(instrumented, timeout, unit);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		Collection<? extends Callable<T>> instrumented = this.instrument(tasks);
		return this.delegate.invokeAny(instrumented);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		Collection<? extends Callable<T>> instrumented = this.instrument(tasks);
		return this.delegate.invokeAny(instrumented, timeout, unit);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> Collection<? extends Callable<T>> instrument(Collection<? extends Callable<T>> tasks) {
		final List<InstrumentedCallable<T>> instrumented = new ArrayList<>(tasks.size());
		for (Callable<T> task : tasks) {
			instrumented.add(new InstrumentedCallable(task));
		}
		return instrumented;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute(Runnable command) {
		this.submitted(command);
		this.delegate.execute(new InstrumentedRunnable(command));
	}

	private class InstrumentedRunnable implements Runnable {
		private final Runnable command;

		InstrumentedRunnable(Runnable command) {
			this.command = command;
		}

		@Override
		public void run() {
			InstrumentedScheduledExecutorService.this.runningInc(this.command);
			try {
				TracingUtils.addNewMdcTraceContext(InstrumentedScheduledExecutorService.this.tracingProperties);
				InstrumentedScheduledExecutorService.this.duration(this.command).record(this.command);
			} finally {
				TracingUtils.removeMdcTraceContext(InstrumentedScheduledExecutorService.this.tracingProperties);

				InstrumentedScheduledExecutorService.this.runningDec(this.command);
				InstrumentedScheduledExecutorService.this.completed(this.command);
			}
		}
	}

	private class InstrumentedPeriodicRunnable implements Runnable {
		private final Runnable command;
		private final long periodInNanos;

		InstrumentedPeriodicRunnable(Runnable command, long period, TimeUnit unit) {
			this.command = command;
			this.periodInNanos = unit.toNanos(period);
		}

		@Override
		public void run() {
			InstrumentedScheduledExecutorService.this.runningInc(this.command);
			StopWatch executionStopWatch = new StopWatch();
			try {
				executionStopWatch.start();
				InstrumentedScheduledExecutorService.this.duration(this.command).record(this.command);
			} finally {
				executionStopWatch.stop();

				InstrumentedScheduledExecutorService.this.runningDec(this.command);
				InstrumentedScheduledExecutorService.this.completed(this.command);
				if (executionStopWatch.getTotalTimeMillis() > this.periodInNanos) {
					InstrumentedScheduledExecutorService.this.scheduledOverrun(this.command);
				}
			}
		}
	}

	private class InstrumentedCallable<T> implements Callable<T> {
		private final Callable<T> task;

		InstrumentedCallable(Callable<T> task) {
			this.task = task;
		}

		@Override
		public T call() throws Exception {
			return this.task.call();
		}
	}
}
