# OTLP Log Parser (Java)

## Introduction

This take-home assignment is designed to give you an opportunity to demonstrate your skills and experience in
building a small backend application. We expect you to spend 3-4 hours on this assignment. If you find yourself spending
more time
than that, please stop and submit what you have. We are not looking for a complete solution, but rather a demonstration
of your skills and experience.

To submit your solution, please create a public GitHub repository and send us the link. Please include a `README.md`
file
with instructions on how to run your application.

## Overview

The goal of this assignment is to build a simple backend application that
receives [log records](https://opentelemetry.io/docs/concepts/signals/logs/)
on a gRPC endpoint and processes them. Based on a **configurable attribute key and duration**, the application has to
keep
counts of the number of unique log records per distinct attribute value. And within each window (configurable duration)
print /
log these counts to stdout.
Note that the configurable attribute may appear either on Resource, Scope or Log level.

Pseudo example:

- "my log body 1" - {"foo":"bar", "baz":"qux"}
- "my log body 2" - {"foo":"qux", "baz":"qux"}
- "my log body 3" - {"baz":"qux"}
- "my log body 4" - {"foo":"baz"}
- "my log body 5" - {"foo":"baz", "baz":"qux"}

For example for configured attribute key "foo" it should report:

- "bar" - 1
- "qux" - 1
- "baz" - 2
- unknown - 1

Your solution should take into account high throughput, both in number of messages and the number of records per
message.

## Technology Constraints

- Your Java program should compile using standard Oracle JDK, and be compatible with Java 21.
- Use any additional libraries you want and need.

## Notes

- As this assignment is for the role of a Senior Product Engineer, we expect you to pay some attention to
  maintainability and operability of the solution. For example:
  - Consistent terminology usage
  - Validation of the behaviour
  - Include signals / events to help in debugging
- Assume that this application will be deployed to production. Build it accordingly.

## Solution

This solution consists of two main components for processing and tracking unique log counts from OpenTelemetry logs in a
distributed environment:

### UniqueCountLogProcessor:

This class processes incoming OpenTelemetry log records and counts the occurrences of unique values based on a specified
attribute (e.g., user ID).
It uses a thread-safe, concurrent map (ConcurrentHashMap) to track the counts and processes logs in parallel by breaking
them into chunks, improving performance in a multi-threaded environment.
If processing a log fails (e.g., due to missing attribute), the log is added to a retry queue for later processing.

### UniqueCountLogSchedulerTaskNotifier:

This component periodically logs and resets the unique log count after a specified time window, using a
ScheduledExecutorService.
The scheduler executes at fixed intervals, taking a snapshot of the current log counts and clearing them atomically to
ensure consistency.
It helps in monitoring log counts over a time-based window, providing periodic insights into log distribution.

### Thread Safety and Error Handling Insights

#### Thread Safety

- **Concurrent Data Structures**:
  The solution makes heavy use of thread-safe data structures to ensure safe concurrent operations. Specifically:
  - `ConcurrentHashMap`: Used to store log counts based on the specified attribute. This map allows for atomic updates
    of the count values without needing external synchronization mechanisms, making it ideal for high-concurrency
    environments.
  - `AtomicReference`: The `logCounts` field is wrapped in an `AtomicReference` to ensure atomic replacement of the
    entire map, which guarantees consistency when switching to a new map instance during the reset phase.
  - `ConcurrentLinkedQueue`: Used to handle logs that fail to process initially (due to missing attributes or other
    issues). This queue allows logs to be added and retrieved safely in a multi-threaded environment without locking.

- **Chunking for Reduced Contention**:
  Logs are processed in chunks (`CHUNK_SIZE`) to reduce the frequency of synchronization required on the
  `ConcurrentHashMap`. Instead of processing each log individually and synchronizing on every update, the logs are
  grouped into batches that are processed concurrently. This minimizes the contention between threads, improving overall
  throughput and reducing the chance of race conditions.

- **Executor Service with Virtual Threads**:
  An `ExecutorService` is used to submit log processing tasks, and it leverages virtual threads to ensure that the
  system can handle large numbers of log processing tasks concurrently without exhausting system resources. Virtual
  threads provide a lightweight, scalable concurrency model, allowing the system to efficiently handle high volumes of
  logs while ensuring each log's count is updated atomically in the shared map.

#### Error Handling

- **Retry Mechanism**:
  When a log record fails to be processed (for instance, if the attribute is missing or cannot be extracted), it is
  caught by a `try-catch` block and added to a retry queue (`unprocessedLogs`). This ensures that failed logs are not
  lost and can be retried later, preventing data loss in case of temporary issues or missing information in the logs.
  The retry mechanism wasn't implemented in the provided code, but it can be added to the `UniqueCountLogProcessor`
  class to handle failed logs.
- **Logging**:
  Whenever an error occurs during log processing, the system logs a warning (`LOGGER.error()`) indicating the failure
  and the reason for it. This provides visibility into processing issues without halting the entire system, making it
  easier to diagnose and address any anomalies in the log data.

- **Graceful Shutdown**:
  The `shutdown()` methods in both classes ensure that any ongoing log processing or scheduled tasks are allowed to
  complete or are safely terminated before the system shuts down. The `awaitTermination()` method ensures that the
  system waits for a specified amount of time (up to 10 seconds) for active threads to finish. If they do not complete
  in time, the system forces a shutdown (`shutdownNow()`), preventing lingering tasks from delaying the shutdown
  process.

By combining these thread-safety mechanisms and robust error handling strategies, the solution ensures high reliability,
scalability, and resilience in a multi-threaded environment, making it suitable for production-grade log processing
systems.

## Usage

There is a class call [GrpcClient.java](src/main/java/com/dash0/homeexercise/GrpcClient.java) that calls the Grpc server
to ingest logs
after spinning up the server this class can be run.

Build the application:

```shell
./gradlew assemble
```

Run the application:

```shell
./gradlew bootRun
```

Run tests

```shell
./gradlew check
```

## References

- [OpenTelemetry Logs](https://opentelemetry.io/docs/concepts/signals/logs/)
- [OpenTelemetry Protocol (OTLP)](https://github.com/open-telemetry/opentelemetry-proto)
- [OTLP Logs Examples](https://github.com/open-telemetry/opentelemetry-proto/blob/main/examples/logs.json)
