// SPDX-FileCopyrightText: Copyright 2023-2024 Dash0 Inc.

package com.dash0.homeexercise.otel.grpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsPartialSuccess;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.logs.v1.LogsServiceGrpc;

public class LogsService extends LogsServiceGrpc.LogsServiceImplBase {
	private static final Logger LOGGER = LoggerFactory.getLogger(LogsService.class);
	private final LogProcessor logProcessor;

	public LogsService(LogProcessor logProcessor) {
		this.logProcessor = logProcessor;
	}

	@Override
	public void export(ExportLogsServiceRequest request, StreamObserver<ExportLogsServiceResponse> responseObserver) {
		try {
			this.logProcessor.processLogs(request);
			responseObserver.onNext(ExportLogsServiceResponse.newBuilder()
					.setPartialSuccess(ExportLogsPartialSuccess.newBuilder()
							.build()).build());
			responseObserver.onCompleted();
		} catch (Exception exception) {
			LOGGER.error("Error processing export logs request ", exception);
			responseObserver.onError(Status.INTERNAL
					.withDescription(exception.getMessage())
					.asRuntimeException());
		}
	}
}
