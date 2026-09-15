package transactionservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.protobuf.Empty;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.env.MockEnvironment;

class GrpcBulkheadTest {
  private final MockEnvironment environment =
      new MockEnvironment()
          .withProperty("grpc.resilience.defaults.bulkhead.max-concurrent-calls", "1")
          .withProperty("grpc.resilience.defaults.bulkhead.stream.max-concurrent-calls", "1")
          .withProperty("grpc.resilience.defaults.record-resource-exhausted", "true");
  private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
  private final GrpcResilience resilience = new GrpcResilience(environment, meters);
  private final List<StreamObserver<Empty>> pending = new CopyOnWriteArrayList<>();
  private final List<ManagedChannel> channels = new CopyOnWriteArrayList<>();
  private Server server;
  private String name;
  private static final MethodDescriptor<Empty, Empty> UNARY =
      method("Read", MethodDescriptor.MethodType.UNARY);
  private static final MethodDescriptor<Empty, Empty> STREAM =
      method("Watch", MethodDescriptor.MethodType.SERVER_STREAMING);

  private static MethodDescriptor<Empty, Empty> method(
      String name, MethodDescriptor.MethodType type) {
    return MethodDescriptor.<Empty, Empty>newBuilder()
        .setFullMethodName("test.Service/" + name)
        .setType(type)
        .setRequestMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
        .setResponseMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
        .build();
  }

  private ManagedChannel channel(String dependency) throws Exception {
    if (server == null) {
      name = InProcessServerBuilder.generateName();
      server =
          InProcessServerBuilder.forName(name)
              .directExecutor()
              .addService(
                  ServerServiceDefinition.builder("test.Service")
                      .addMethod(
                          UNARY,
                          ServerCalls.asyncUnaryCall(
                              (Empty request, StreamObserver<Empty> observer) ->
                                  pending.add(observer)))
                      .addMethod(
                          STREAM,
                          ServerCalls.asyncServerStreamingCall(
                              (Empty request, StreamObserver<Empty> observer) ->
                                  pending.add(observer)))
                      .build())
              .build()
              .start();
    }
    var channel =
        InProcessChannelBuilder.forName(name)
            .directExecutor()
            .disableRetry()
            .intercept(resilience.interceptor(dependency))
            .build();
    channels.add(channel);
    return channel;
  }

  @AfterEach
  void close() throws Exception {
    for (var channel : channels) {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (server != null) {
      server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
    meters.close();
  }

  @ParameterizedTest
  @EnumSource(
      value = Status.Code.class,
      names = {"OK", "INTERNAL", "DEADLINE_EXCEEDED", "CANCELLED"})
  void saturationRejectsImmediatelyAndTerminalStatusRestoresPermit(Status.Code code)
      throws Exception {
    var channel = channel("account");
    var first =
        ClientCalls.futureUnaryCall(
            channel.newCall(UNARY, CallOptions.DEFAULT), Empty.getDefaultInstance());
    long started = System.nanoTime();
    assertThatThrownBy(
            () ->
                ClientCalls.blockingUnaryCall(
                    channel,
                    UNARY,
                    CallOptions.DEFAULT.withDeadlineAfter(5, TimeUnit.SECONDS),
                    Empty.getDefaultInstance()))
        .isInstanceOfSatisfying(
            StatusRuntimeException.class,
            e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.RESOURCE_EXHAUSTED));
    assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isLessThan(250);
    assertThat(pending).hasSize(1);
    assertThat(resilience.breaker("account").getMetrics().getNumberOfBufferedCalls()).isZero();
    assertThat(
            meters
                .get("grpc.client.bulkhead.rejected")
                .tag("name", "account-grpc")
                .counter()
                .count())
        .isEqualTo(1);
    assertThat(
            meters
                .get("resilience4j.bulkhead.available.concurrent.calls")
                .tag("name", "account-grpc")
                .gauge()
                .value())
        .isZero();
    assertThat(
            meters
                .get("resilience4j.bulkhead.max.allowed.concurrent.calls")
                .tag("name", "account-grpc")
                .gauge()
                .value())
        .isEqualTo(1);
    if (code == Status.Code.OK) {
      pending.get(0).onNext(Empty.getDefaultInstance());
      pending.get(0).onCompleted();
      first.get(1, TimeUnit.SECONDS);
    } else {
      pending.get(0).onError(Status.fromCode(code).asRuntimeException());
      assertThatThrownBy(() -> first.get(1, TimeUnit.SECONDS))
          .hasCauseInstanceOf(StatusRuntimeException.class);
    }
    assertThat(resilience.bulkhead("account", false).getMetrics().getAvailableConcurrentCalls())
        .isEqualTo(1);
    var next =
        ClientCalls.futureUnaryCall(
            channel.newCall(UNARY, CallOptions.DEFAULT), Empty.getDefaultInstance());
    assertThat(pending).hasSize(2);
    next.cancel(true);
    assertThat(resilience.bulkhead("account", false).getMetrics().getAvailableConcurrentCalls())
        .isEqualTo(1);
  }

  @Test
  void realDeadlineReleasesPermit() throws Exception {
    var channel = channel("account");
    var result =
        ClientCalls.futureUnaryCall(
            channel.newCall(
                UNARY, CallOptions.DEFAULT.withDeadlineAfter(100, TimeUnit.MILLISECONDS)),
            Empty.getDefaultInstance());
    assertThatThrownBy(() -> result.get(1, TimeUnit.SECONDS))
        .hasCauseInstanceOf(StatusRuntimeException.class);
    assertThat(resilience.bulkhead("account", false).getMetrics().getAvailableConcurrentCalls())
        .isEqualTo(1);
  }

  @Test
  void saturatedAccountDoesNotBlockCardAndInstancesAreShared() throws Exception {
    var account = channel("account");
    var card = channel("card");
    var first =
        ClientCalls.futureUnaryCall(
            account.newCall(UNARY, CallOptions.DEFAULT), Empty.getDefaultInstance());
    var second =
        ClientCalls.futureUnaryCall(
            card.newCall(UNARY, CallOptions.DEFAULT), Empty.getDefaultInstance());
    assertThat(pending).hasSize(2);
    assertThat(resilience.bulkhead("account", false))
        .isSameAs(resilience.bulkhead("account", false));
    assertThat(resilience.bulkhead("account", false))
        .isNotSameAs(resilience.bulkhead("card", false));
    first.cancel(true);
    second.cancel(true);
  }

  @Test
  void streamKeepsItsSeparatePermitUntilCancellation() throws Exception {
    var channel = channel("transaction");
    var call = channel.newCall(STREAM, CallOptions.DEFAULT);
    ClientCalls.asyncServerStreamingCall(
        call,
        Empty.getDefaultInstance(),
        new StreamObserver<>() {
          public void onNext(Empty value) {}

          public void onError(Throwable error) {}

          public void onCompleted() {}
        });
    pending.get(0).onNext(Empty.getDefaultInstance());
    assertThat(resilience.bulkhead("transaction", true).getMetrics().getAvailableConcurrentCalls())
        .isZero();
    var unary =
        ClientCalls.futureUnaryCall(
            channel.newCall(UNARY, CallOptions.DEFAULT), Empty.getDefaultInstance());
    assertThat(pending).hasSize(2);
    call.cancel("unsubscribed", null);
    unary.cancel(true);
    assertThat(resilience.bulkhead("transaction", true).getMetrics().getAvailableConcurrentCalls())
        .isEqualTo(1);
  }

  @Test
  void saturationDoesNotConsumeHalfOpenProbe() throws Exception {
    var channel = channel("account");
    var bulkhead = resilience.bulkhead("account", false);
    bulkhead.acquirePermission();
    var breaker = resilience.breaker("account");
    breaker.transitionToOpenState();
    breaker.transitionToHalfOpenState();
    assertThatThrownBy(
            () ->
                ClientCalls.blockingUnaryCall(
                    channel, UNARY, CallOptions.DEFAULT, Empty.getDefaultInstance()))
        .hasMessageContaining("bulkhead saturated");
    assertThat(breaker.getMetrics().getNumberOfBufferedCalls()).isZero();
    bulkhead.onComplete();
    for (int i = 0; i < 3; i++) {
      var result =
          ClientCalls.futureUnaryCall(
              channel.newCall(UNARY, CallOptions.DEFAULT), Empty.getDefaultInstance());
      pending.get(i).onNext(Empty.getDefaultInstance());
      pending.get(i).onCompleted();
      result.get(1, TimeUnit.SECONDS);
    }
    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
  }

  @Test
  @SuppressWarnings("unchecked")
  void synchronousStartFailureReleasesPermitExactlyOnce() {
    Channel transport = mock(Channel.class);
    ClientCall<Empty, Empty> delegate = mock(ClientCall.class);
    when(transport.newCall(UNARY, CallOptions.DEFAULT)).thenReturn(delegate);
    doThrow(new IllegalStateException("start failed")).when(delegate).start(any(), any());
    var call =
        resilience.interceptor("account").interceptCall(UNARY, CallOptions.DEFAULT, transport);
    assertThatThrownBy(() -> call.start(new ClientCall.Listener<>() {}, new Metadata()))
        .hasMessage("start failed");
    call.cancel("cleanup", null);
    assertThat(resilience.bulkhead("account", false).getMetrics().getAvailableConcurrentCalls())
        .isEqualTo(1);
  }

  @Test
  void rejectsWaitingConfigurationAndSupportsPerDependencyLimits() {
    environment.withProperty("grpc.resilience.instances.card.bulkhead.max-concurrent-calls", "3");
    assertThat(resilience.bulkhead("card", false).getMetrics().getMaxAllowedConcurrentCalls())
        .isEqualTo(3);
    environment.withProperty("grpc.resilience.instances.user.bulkhead.max-wait-duration-ms", "100");
    assertThatThrownBy(() -> resilience.bulkhead("user", false))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
