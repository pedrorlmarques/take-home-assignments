package com.dash0.homeexercise.otel.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Arrays;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.proto.resource.v1.Resource;

class UniqueCountLogProcessorTest {

	@Test
	void testGivenLogRecordRequestItShouldCalculateUniqueLogRecordByAttributeKey() {

		var logRecord1 = LogRecord.newBuilder()
				.setTimeUnixNano(1680000000000L)
				.setSeverityText("INFO")
				.setBody(AnyValue.newBuilder()
						.setStringValue("my log body 1")
						.build())
				.addAttributes(KeyValue.newBuilder()
						.setKey("foo")
						.setValue(AnyValue.newBuilder()
								.setStringValue("bar")
								.build())
						.build())
				.addAttributes(KeyValue.newBuilder()
						.setKey("baz")
						.setValue(AnyValue.newBuilder()
								.setStringValue("qux")
								.build())
						.build())
				.build();

		var logRecord2 = LogRecord.newBuilder()
				.setTimeUnixNano(1680000001000L)
				.setSeverityText("INFO")
				.setBody(AnyValue.newBuilder()
						.setStringValue("my log body 2")
						.build())
				.addAttributes(KeyValue.newBuilder()
						.setKey("foo")
						.setValue(AnyValue.newBuilder()
								.setStringValue("qux")
								.build())
						.build())
				.addAttributes(KeyValue.newBuilder()
						.setKey("baz")
						.setValue(AnyValue.newBuilder()
								.setStringValue("qux")
								.build())
						.build())
				.build();

		var logRecord3 = LogRecord.newBuilder()
				.setTimeUnixNano(1680000001000L)
				.setSeverityText("INFO")
				.setBody(AnyValue.newBuilder()
						.setStringValue("my log body 3")
						.build())
				.addAttributes(KeyValue.newBuilder()
						.setKey("baz")
						.setValue(AnyValue.newBuilder()
								.setStringValue("qux")
								.build())
						.build())
				.build();

		var logRecord4 = LogRecord.newBuilder()
				.setTimeUnixNano(1680000001000L)
				.setSeverityText("INFO")
				.setBody(AnyValue.newBuilder()
						.setStringValue("my log body 4")
						.build())
				.addAttributes(KeyValue.newBuilder()
						.setKey("foo")
						.setValue(AnyValue.newBuilder()
								.setStringValue("baz")
								.build())
						.build())
				.build();

		var logRecord5 = LogRecord.newBuilder()
				.setTimeUnixNano(1680000001000L)
				.setSeverityText("INFO")
				.setBody(AnyValue.newBuilder()
						.setStringValue("my log body 5")
						.build())
				.addAttributes(KeyValue.newBuilder()
						.setKey("foo")
						.setValue(AnyValue.newBuilder()
								.setStringValue("baz")
								.build())
						.build())
				.addAttributes(KeyValue.newBuilder()
						.setKey("baz")
						.setValue(AnyValue.newBuilder()
								.setStringValue("qux")
								.build())
						.build())
				.build();


		// Create scope logs (can be adjusted per log group)
		var scopeLogs = ScopeLogs.newBuilder()
				.setScope(InstrumentationScope.newBuilder().setName("my-logging-library").setVersion("1.0").build())
				.addAllLogRecords(Arrays.asList(logRecord1, logRecord2, logRecord3, logRecord4, logRecord5))
				.build();

		// Create resource logs (add resource attributes here)
		var resourceLogs = ResourceLogs.newBuilder()
				.setResource(
						Resource
								.newBuilder()
								.addAttributes(
										KeyValue.newBuilder().setKey("service.name")
												.setValue(AnyValue.newBuilder().setStringValue("my-service").build()).build()
								).build()
				)
				.addScopeLogs(scopeLogs)
				.build();

		// Create ExportLogsServiceRequest
		var exportLogsServiceRequest = ExportLogsServiceRequest.newBuilder()
				.addResourceLogs(resourceLogs)
				.build();


		var uniqueCountLogProcessor = new UniqueCountLogProcessor(Executors.newVirtualThreadPerTaskExecutor(), "foo");
		uniqueCountLogProcessor.processLogs(exportLogsServiceRequest);

		await().untilAsserted(() ->
				assertThat(uniqueCountLogProcessor.getLogCounts().get())
						.containsEntry("bar", 1)
						.containsEntry("qux", 1)
						.containsEntry("baz", 2)
						.containsEntry("unknown", 1));
	}
}
