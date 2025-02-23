package com.dash0.homeexercise;

import java.util.ArrayList;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.LogsServiceGrpc;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.proto.resource.v1.Resource;

/**
 * Auxiliary class to send logs to the Grpc server.
 */
public class GrpcClient {

	public static void main(String[] args) {
		// Create a channel to the server (localhost and PORT)
		ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 4317)
				.usePlaintext() // Disable SSL (if needed)
				.build();

		// Create a blocking stub to call the service
		var stub = LogsServiceGrpc.newBlockingStub(channel);

		//// Create a request message
		//LogRecord logRecord1 = LogRecord.newBuilder()
		//		.setTimeUnixNano(1680000000000L)
		//		.setSeverityText("INFO")
		//		.setBody(AnyValue.newBuilder()
		//				.setStringValue("my log body 1")
		//				.build())
		//		.addAttributes(KeyValue.newBuilder()
		//				.setKey("foo")
		//				.setValue(AnyValue.newBuilder()
		//						.setStringValue("bar")
		//						.build())
		//				.build())
		//		.addAttributes(KeyValue.newBuilder()
		//				.setKey("baz")
		//				.setValue(AnyValue.newBuilder()
		//						.setStringValue("qux")
		//						.build())
		//				.build())
		//		.build();
		//
		//LogRecord logRecord2 = LogRecord.newBuilder()
		//		.setTimeUnixNano(1680000001000L)
		//		.setSeverityText("INFO")
		//		.setBody(AnyValue.newBuilder()
		//				.setStringValue("my log body 2")
		//				.build())
		//		.addAttributes(KeyValue.newBuilder()
		//				.setKey("foo")
		//				.setValue(AnyValue.newBuilder()
		//						.setStringValue("qux")
		//						.build())
		//				.build())
		//		.addAttributes(KeyValue.newBuilder()
		//				.setKey("baz")
		//				.setValue(AnyValue.newBuilder()
		//						.setStringValue("qux")
		//						.build())
		//				.build())
		//		.build();
		//
		//LogRecord logRecord3 = LogRecord.newBuilder()
		//		.setTimeUnixNano(1680000001000L)
		//		.setSeverityText("INFO")
		//		.setBody(AnyValue.newBuilder()
		//				.setStringValue("my log body 3")
		//				.build())
		//		.addAttributes(KeyValue.newBuilder()
		//				.setKey("baz")
		//				.setValue(AnyValue.newBuilder()
		//						.setStringValue("qux")
		//						.build())
		//				.build())
		//		.build();
		//
		//LogRecord logRecord4 = LogRecord.newBuilder()
		//		.setTimeUnixNano(1680000001000L)
		//		.setSeverityText("INFO")
		//		.setBody(AnyValue.newBuilder()
		//				.setStringValue("my log body 4")
		//				.build())
		//		.addAttributes(KeyValue.newBuilder()
		//				.setKey("foo")
		//				.setValue(AnyValue.newBuilder()
		//						.setStringValue("baz")
		//						.build())
		//				.build())
		//		.build();
		//
		//LogRecord logRecord5 = LogRecord.newBuilder()
		//		.setTimeUnixNano(1680000001000L)
		//		.setSeverityText("INFO")
		//		.setBody(AnyValue.newBuilder()
		//				.setStringValue("my log body 5")
		//				.build())
		//		.addAttributes(KeyValue.newBuilder()
		//				.setKey("foo")
		//				.setValue(AnyValue.newBuilder()
		//						.setStringValue("baz")
		//						.build())
		//				.build())
		//		.addAttributes(KeyValue.newBuilder()
		//				.setKey("baz")
		//				.setValue(AnyValue.newBuilder()
		//						.setStringValue("qux")
		//						.build())
		//				.build())
		//		.build();


		List<LogRecord> logRecords = new ArrayList<>();

		for (int i = 1; i <= 100; i++) {
			LogRecord.Builder logRecordBuilder = LogRecord.newBuilder()
					.setTimeUnixNano(1680000000000L + (i * 1000L))
					.setSeverityText("INFO")
					.setBody(AnyValue.newBuilder()
							.setStringValue("my log body " + i)
							.build());

			if (i % 2 == 0) {
				logRecordBuilder.addAttributes(KeyValue.newBuilder()
						.setKey("foo")
						.setValue(AnyValue.newBuilder()
								.setStringValue("bar")
								.build())
						.build());
			}
			if (i % 3 == 0) {
				logRecordBuilder.addAttributes(KeyValue.newBuilder()
						.setKey("baz")
						.setValue(AnyValue.newBuilder()
								.setStringValue("qux")
								.build())
						.build());
			}
			if (i % 5 == 0) {
				logRecordBuilder.addAttributes(KeyValue.newBuilder()
						.setKey("foo")
						.setValue(AnyValue.newBuilder()
								.setStringValue("baz")
								.build())
						.build());
			}
			logRecords.add(logRecordBuilder.build());
		}

		// Create scope logs (can be adjusted per log group)
		ScopeLogs scopeLogs = ScopeLogs.newBuilder()
				.setScope(InstrumentationScope.newBuilder().setName("my-logging-library").setVersion("1.0").build())
				.addAllLogRecords(logRecords)
				.build();

		// Create resource logs (add resource attributes here)
		ResourceLogs serviceAReplica1 = ResourceLogs.newBuilder()
				.setResource(
						Resource
								.newBuilder()
								.addAttributes(
										KeyValue.newBuilder().setKey("service.name")
												.setValue(AnyValue.newBuilder().setStringValue("service-A").build()).build()
								).build()
				)
				.addScopeLogs(scopeLogs)
				.build();

		//ResourceLogs serviceAReplica2 = ResourceLogs.newBuilder()
		//		.setResource(
		//				Resource
		//						.newBuilder()
		//						.addAttributes(
		//								KeyValue.newBuilder().setKey("service.name")
		//										.setValue(AnyValue.newBuilder().setStringValue("service-A").build()).build()
		//						).build()
		//		)
		//		.addScopeLogs(scopeLogs)
		//		.build();

		// Create ExportLogsServiceRequest
		var exportLogsServiceRequest = ExportLogsServiceRequest.newBuilder()
				//.addAllResourceLogs(List.of(serviceAReplica1, serviceAReplica2))
				.addAllResourceLogs(List.of(serviceAReplica1))
				.build();

		// Call the service method and get the response
		var response = stub.export(exportLogsServiceRequest);

		// Process the response
		System.out.println("Response: " + response);

		// Shutdown the channel after use
		channel.shutdown();
	}
}
