package com.dash0.homeexercise.otel.grpc;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PreDestroy;

/**
 * Notifier for logging unique count of logs in a specified window duration.
 */
public class UniqueCountLogSchedulerTaskNotifier {

	private static final Logger LOGGER = LoggerFactory.getLogger(UniqueCountLogSchedulerTaskNotifier.class);
	private final UniqueCountLogProcessor uniqueCountLogProcessor;
	private final ScheduledExecutorService scheduler;
	private final Duration windowDuration;

	public UniqueCountLogSchedulerTaskNotifier(UniqueCountLogProcessor uniqueCountLogProcessor,
			ScheduledExecutorService scheduler,
			Duration windowDuration) {
		this.uniqueCountLogProcessor = uniqueCountLogProcessor;
		this.scheduler = scheduler;
		this.windowDuration = windowDuration;

		this.scheduler.scheduleAtFixedRate(this::safeLogAndClearCounts,
				windowDuration.toSeconds(), windowDuration.toSeconds(), TimeUnit.SECONDS);
		LOGGER.info("Log Count Scheduler initialized with windowDuration={}s", windowDuration.toSeconds());
	}

	private void safeLogAndClearCounts() {

		// Get the current log counts snapshot
		var logCountsSnapshot = new ConcurrentHashMap<>(uniqueCountLogProcessor.getLogCounts().get());

		if (logCountsSnapshot.isEmpty()) {
			return;
		}

		// Clear the map atomically by replacing it with a new map
		uniqueCountLogProcessor.getLogCounts().set(new ConcurrentHashMap<>());

		LOGGER.info("Unique log counts (window={}s): {}", windowDuration.toSeconds(), logCountsSnapshot);
	}

	// Shutdown the scheduler executor service
	@PreDestroy
	public void shutdown() {
		LOGGER.info("Shutting down Unique Count Log Scheduler...");
		scheduler.shutdown();
		try {
			if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			LOGGER.error("Error shutting down scheduler", e);
			Thread.currentThread().interrupt();
		}
		LOGGER.info("Unique Count Log Scheduler shutdown complete.");
	}
}
