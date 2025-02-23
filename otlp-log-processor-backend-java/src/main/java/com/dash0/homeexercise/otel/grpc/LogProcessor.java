package com.dash0.homeexercise.otel.grpc;

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;

/**
 * Interface for processing logs.
 */
public interface LogProcessor {
	void processLogs(ExportLogsServiceRequest request);
}
