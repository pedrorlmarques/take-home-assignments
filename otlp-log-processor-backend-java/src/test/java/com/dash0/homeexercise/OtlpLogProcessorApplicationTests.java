// SPDX-FileCopyrightText: Copyright 2023 Dash0 Inc.

package com.dash0.homeexercise;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import com.dash0.homeexercise.otel.grpc.GrpcConfig;
import com.dash0.homeexercise.otel.grpc.GrpcServer;
import com.dash0.homeexercise.util.GenericContextInitializerHook;
import com.dash0.homeexercise.util.OtelTestConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Import(OtelTestConfiguration.class)
@ContextConfiguration(initializers = {GenericContextInitializerHook.class})
class OtlpLogProcessorApplicationTests {

	@Autowired
	OpenTelemetryExtension otelExtension;
	@Autowired
	GrpcServer grpcServer;
	@Autowired
	GrpcConfig grpcConfig;


	@Test
	void testLogsIngestion() {
		OtlpGrpcLogRecordExporter logExporter =
				OtlpGrpcLogRecordExporter.builder().setEndpoint("http://localhost:" + grpcConfig.getListenPort()).build();

		// Build the OpenTelemetry BatchLogRecordProcessor with the GrpcExporter
		BatchLogRecordProcessor logRecordProcessor = BatchLogRecordProcessor.builder(logExporter).build();

		// Add the logRecord processor to the default TracerSdkProvider
		SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder().addLogRecordProcessor(logRecordProcessor).build();

		try (OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder().setLoggerProvider(loggerProvider).build()) {

			// Create an OpenTelemetry Logger
			var logger = loggerProvider.get("opentel-example");

			// Create a basic log record
			// Create a few basic log records

			var attributeToTrack = "some-attribute";
			logger.logRecordBuilder()
					.setBody("first message")
					.setSeverity(Severity.INFO)
					.setAttribute(AttributeKey.stringKey(attributeToTrack), "foo")
					.emit();
			logger.logRecordBuilder()
					.setBody("second message")
					.setSeverity(Severity.INFO)
					.setAttribute(AttributeKey.stringKey(attributeToTrack), "foo")
					.emit();
			logger.logRecordBuilder()
					.setBody("third message")
					.setSeverity(Severity.INFO)
					.setAttribute(AttributeKey.stringKey("some-other-attribute"), "foo")
					.emit();
			logger.logRecordBuilder()
					.setBody("fourth message")
					.setSeverity(Severity.INFO)
					.setAttribute(AttributeKey.stringKey(attributeToTrack), "baz")
					.emit();

			CompletableResultCode flushResult = logRecordProcessor.forceFlush().join(5, TimeUnit.SECONDS);
			assertThat(flushResult.isSuccess() || !flushResult.isDone()).isTrue();
			assertThat(logExporter.flush().isSuccess()).isTrue();

			openTelemetrySdk.shutdown();
		}

		// Meter does not exist yet
		//assertThat(
		//		otelExtension.getMetrics().stream()
		//				.filter(metricData -> "grpc.logs.received.count".equals(metricData.getName()))
		//				.map(MetricData::getData)
		//				.flatMap(data -> data.getPoints().stream())
		//				.map(point -> ((LongPointData) point).getValue())
		//				.max(Comparator.naturalOrder()).get())
		//		.isGreaterThan(0);
	}

}
