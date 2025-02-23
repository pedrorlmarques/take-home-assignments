package com.dash0.homeexercise.otel.grpc;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import jakarta.annotation.PreDestroy;

/**
 * Log processor that counts the number of unique log records based on a specified attribute.
 */
public class UniqueCountLogProcessor implements LogProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(UniqueCountLogProcessor.class);
	private static final int CHUNK_SIZE = 10;
	// This ensures thread-safe, atomic updates when replacing the entire map with a new instance (new ConcurrentHashMap<>()).
	private final AtomicReference<ConcurrentHashMap<String, Integer>> logCounts;
	// This is a non-blocking, thread-safe queue that allows multiple threads to safely add and remove elements without external synchronization.
	private final ConcurrentLinkedQueue<LogRecord> unprocessedLogs = new ConcurrentLinkedQueue<>();
	private final ExecutorService executor;
	private final String attributeKey;

	public UniqueCountLogProcessor(ExecutorService executor, String attributeKey) {
		this.attributeKey = attributeKey;
		this.executor = executor;
		this.logCounts = new AtomicReference<>(new ConcurrentHashMap<>());
	}

	public AtomicReference<ConcurrentHashMap<String, Integer>> getLogCounts() {
		return logCounts;
	}

	@Override
	public void processLogs(ExportLogsServiceRequest request) {

		var logs = request.getResourceLogsList();

		if (logs.isEmpty()) return;

		for (ResourceLogs resourceLog : logs) {
			for (ScopeLogs scopeLog : resourceLog.getScopeLogsList()) {
				// Instead of handling logs one by one, we process them in chunks of CHUNK_SIZE.
				// Reduces the number of synchronization points in ConcurrentHashMap
				// Tasks are submitted in batches to a virtual thread
				var logRecordsChunk = new Chunk<>(scopeLog.getLogRecordsList(), CHUNK_SIZE);
				LOGGER.info("Will process {} Log Records in {} chunks", scopeLog.getLogRecordsList().size(),
						logRecordsChunk.size());

				for (List<LogRecord> chunk : logRecordsChunk) {
					executor.submit(() -> processLogRecordsChunk(chunk, resourceLog, scopeLog));
				}
			}
		}
	}

	private void processLogRecordsChunk(List<LogRecord> logRecordsChunk, ResourceLogs resourceLog, ScopeLogs scopeLog) {
		for (LogRecord logRecord : logRecordsChunk) {
			try {
				String attributeValue = extractAttribute(resourceLog, scopeLog, logRecord);
				logCounts.get().compute(attributeValue, (key, count) -> (count == null) ? 1 : count + 1);
			} catch (Exception e) {
				LOGGER.warn("Failed to process log record, adding to retry queue", e);
				unprocessedLogs.offer(logRecord);
			}
		}
	}

	// extract attribute value from log record, scope log, or resource log
	private String extractAttribute(ResourceLogs resourceLog, ScopeLogs scopeLog, LogRecord logRecord) {
		var attribute = getAttributeFromList(logRecord.getAttributesList());
		if (attribute == null) {
			attribute = getAttributeFromList(scopeLog.getScope().getAttributesList());
		}
		if (attribute == null) {
			attribute = getAttributeFromList(resourceLog.getResource().getAttributesList());
		}
		return attribute != null ? attribute : "unknown";
	}

	private String getAttributeFromList(List<KeyValue> attributes) {
		return attributes.stream()
				.filter(attr -> attr.getKey().equals(attributeKey))
				.map(KeyValue::getValue)
				.map(AnyValue::getStringValue)
				.findFirst()
				.orElse(null);
	}

	// Shutdown the executor service
	@PreDestroy
	public void shutdown() {

		LOGGER.info("Shutting down Log Processor...");
		try {
			executor.shutdown();
			if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			LOGGER.error("Error shutting down executors", e);
			Thread.currentThread().interrupt();
		}

		LOGGER.info("Log Processor shutdown complete.");
	}
}
