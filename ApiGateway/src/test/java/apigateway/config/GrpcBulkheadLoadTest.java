package apigateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Empty;
import io.grpc.CallOptions;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.mock.env.MockEnvironment;

/** Repeatable synthetic capacity sweep; does not measure the real bank services or databases. */
@EnabledIfSystemProperty(named = "grpc.bulkhead.load", matches = "true")
class GrpcBulkheadLoadTest {
  @Test
  void sweepConcurrencyAgainstEightWorkerDownstream() throws Exception {
    var rows = new ArrayList<String>();
    rows.add(
        "limit,requests,success,rejected,max_client_concurrency,max_server_concurrency,success_p95_ms,rejection_p95_ms,healthy_rpc_ms");
    for (int limit : List.of(4, 8, 16, 32)) {
      rows.add(run(limit));
    }
    Files.write(Path.of("target/bulkhead-load.csv"), rows);
  }

  private String run(int limit) throws Exception {
    var env =
        new MockEnvironment()
            .withProperty(
                "grpc.resilience.defaults.bulkhead.max-concurrent-calls", Integer.toString(limit));
    var meters = new SimpleMeterRegistry();
    var resilience = new GrpcResilience(env, meters);
    var peak = new AtomicInteger();
    var serverActive = new AtomicInteger();
    var serverPeak = new AtomicInteger();
    resilience
        .bulkhead("account", false)
        .getEventPublisher()
        .onCallPermitted(
            event ->
                peak.accumulateAndGet(
                    limit
                        - resilience
                            .bulkhead("account", false)
                            .getMetrics()
                            .getAvailableConcurrentCalls(),
                    Math::max));
    var method =
        MethodDescriptor.<Empty, Empty>newBuilder()
            .setFullMethodName("load.Service/Read")
            .setType(MethodDescriptor.MethodType.UNARY)
            .setRequestMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
            .setResponseMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
            .build();
    String name = InProcessServerBuilder.generateName();
    var serverWorkers = Executors.newFixedThreadPool(8);
    var server =
        InProcessServerBuilder.forName(name)
            .executor(serverWorkers)
            .addService(
                ServerServiceDefinition.builder("load.Service")
                    .addMethod(
                        method,
                        ServerCalls.asyncUnaryCall(
                            (Empty request, StreamObserver<Empty> observer) -> {
                              serverPeak.accumulateAndGet(
                                  serverActive.incrementAndGet(), Math::max);
                              try {
                                Thread.sleep(200);
                                observer.onNext(Empty.getDefaultInstance());
                                observer.onCompleted();
                              } catch (InterruptedException interrupted) {
                                Thread.currentThread().interrupt();
                                observer.onError(Status.CANCELLED.asRuntimeException());
                              } finally {
                                serverActive.decrementAndGet();
                              }
                            }))
                    .build())
            .build()
            .start();
    var account =
        InProcessChannelBuilder.forName(name)
            .directExecutor()
            .disableRetry()
            .intercept(resilience.interceptor("account"))
            .build();
    // Separate healthy downstream and semaphore, sharing the 64-thread caller pool.
    String healthyName = InProcessServerBuilder.generateName();
    var healthyServer =
        InProcessServerBuilder.forName(healthyName)
            .directExecutor()
            .addService(
                ServerServiceDefinition.builder("load.Service")
                    .addMethod(
                        method,
                        ServerCalls.asyncUnaryCall(
                            (Empty request, StreamObserver<Empty> observer) -> {
                              observer.onNext(Empty.getDefaultInstance());
                              observer.onCompleted();
                            }))
                    .build())
            .build()
            .start();
    var card =
        InProcessChannelBuilder.forName(healthyName)
            .directExecutor()
            .disableRetry()
            .intercept(resilience.interceptor("card"))
            .build();
    var callers = Executors.newFixedThreadPool(64);
    List<Long> successes = Collections.synchronizedList(new ArrayList<>());
    List<Long> rejections = Collections.synchronizedList(new ArrayList<>());
    long healthyMax = 0;
    try {
      for (int wave = 0; wave < 3; wave++) {
        var ready = new CountDownLatch(64);
        var start = new CountDownLatch(1);
        var admitted = new CountDownLatch(1);
        resilience
            .bulkhead("account", false)
            .getEventPublisher()
            .onCallPermitted(event -> admitted.countDown());
        var futures = new ArrayList<java.util.concurrent.Future<?>>();
        for (int i = 0; i < 64; i++) {
          futures.add(
              callers.submit(
                  () -> {
                    ready.countDown();
                    try {
                      start.await();
                      long began = System.nanoTime();
                      try {
                        ClientCalls.blockingUnaryCall(
                            account,
                            method,
                            CallOptions.DEFAULT.withDeadlineAfter(2, TimeUnit.SECONDS),
                            Empty.getDefaultInstance());
                        successes.add(System.nanoTime() - began);
                      } catch (StatusRuntimeException failure) {
                        assertThat(failure.getStatus().getCode())
                            .isEqualTo(Status.Code.RESOURCE_EXHAUSTED);
                        rejections.add(System.nanoTime() - began);
                      }
                    } catch (InterruptedException interrupted) {
                      Thread.currentThread().interrupt();
                      throw new IllegalStateException(interrupted);
                    }
                  }));
        }
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        assertThat(admitted.await(1, TimeUnit.SECONDS)).isTrue();
        long began = System.nanoTime();
        callers
            .submit(
                () ->
                    ClientCalls.blockingUnaryCall(
                        card,
                        method,
                        CallOptions.DEFAULT.withDeadlineAfter(2, TimeUnit.SECONDS),
                        Empty.getDefaultInstance()))
            .get(2, TimeUnit.SECONDS);
        healthyMax = Math.max(healthyMax, System.nanoTime() - began);
        for (var future : futures) {
          future.get(3, TimeUnit.SECONDS);
        }
      }
      assertThat(peak.get()).isLessThanOrEqualTo(limit);
      assertThat(serverPeak.get()).isLessThanOrEqualTo(8);
      assertThat(resilience.bulkhead("account", false).getMetrics().getAvailableConcurrentCalls())
          .isEqualTo(limit);
      assertThat(rejections).isNotEmpty();
      return String.format(
          java.util.Locale.ROOT,
          "%d,192,%d,%d,%d,%d,%.2f,%.2f,%.2f",
          limit,
          successes.size(),
          rejections.size(),
          peak.get(),
          serverPeak.get(),
          percentile(successes),
          percentile(rejections),
          healthyMax / 1_000_000.0);
    } finally {
      callers.shutdownNow();
      account.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      card.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      healthyServer.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      serverWorkers.shutdownNow();
      meters.close();
    }
  }

  private double percentile(List<Long> samples) {
    var sorted = samples.stream().sorted().toList();
    return sorted.get((int) Math.ceil(sorted.size() * 0.95) - 1) / 1_000_000.0;
  }
}
