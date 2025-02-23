package com.dash0.homeexercise.otel.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UniqueCountLogSchedulerTaskNotifierTest {

	@Test
	void testTaskNotifier() {
		var uniqueCountLogProcessor = Mockito.mock(UniqueCountLogProcessor.class);

		new UniqueCountLogSchedulerTaskNotifier(
				uniqueCountLogProcessor,
				Executors.newSingleThreadScheduledExecutor(),
				Duration.ofSeconds(5));

		var mapAtomicReference = new AtomicReference<ConcurrentHashMap<String, Integer>>();
		mapAtomicReference.set(new ConcurrentHashMap<>(Map.of("foo", 1, "bar", 2)));
		Mockito.when(uniqueCountLogProcessor.getLogCounts()).thenReturn(mapAtomicReference);

		await().untilAsserted(() -> {
			assertThat(mapAtomicReference.get()).isEmpty();
		});
	}

}
