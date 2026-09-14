package apigateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.contract.v1.AccountRpcServiceGrpc;
import com.google.protobuf.Empty;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.env.MockEnvironment;

class GrpcResilienceTest {
  private final MockEnvironment environment =
      new MockEnvironment()
          .withProperty("grpc.resilience.defaults.minimum-calls", "2")
          .withProperty("grpc.resilience.defaults.window-size", "2")
          .withProperty("grpc.resilience.defaults.half-open-calls", "2")
          .withProperty("grpc.resilience.defaults.open-wait-ms", "150")
          .withProperty("grpc.resilience.defaults.retry.initial-backoff-ms", "1");
  private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
  private final GrpcResilience resilience = new GrpcResilience(environment, meters);
  private final CircuitBreaker breaker = resilience.breaker("account");
  private final AtomicInteger requests = new AtomicInteger();
  private final List<StreamObserver<Empty>> pending = new ArrayList<>();
  private Status response = Status.OK;
  private boolean hold;
  private boolean recoverOnRetry;
  private Server server;
  private ManagedChannel channel;

  private MethodDescriptor<Empty, Empty> method(String name, MethodDescriptor.MethodType type) {
    return MethodDescriptor.<Empty, Empty>newBuilder()
        .setFullMethodName("account.v1.AccountRpcService/" + name)
        .setType(type)
        .setRequestMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
        .setResponseMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
        .build();
  }

  private MethodDescriptor<Empty, Empty> start(String name, boolean retry) throws Exception {
    var method = method(name, MethodDescriptor.MethodType.UNARY);
    String serverName = InProcessServerBuilder.generateName();
    server =
        InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(
                ServerServiceDefinition.builder("account.v1.AccountRpcService")
                    .addMethod(
                        method,
                        ServerCalls.asyncUnaryCall(
                            (Empty request, StreamObserver<Empty> observer) -> {
                              int attempt = requests.incrementAndGet();
                              if (recoverOnRetry && attempt > 1) {
                                response = Status.OK;
                              }
                              if (hold) {
                                pending.add(observer);
                              } else if (response.isOk()) {
                                observer.onNext(Empty.getDefaultInstance());
                                observer.onCompleted();
                              } else {
                                observer.onError(response.asRuntimeException());
                              }
                            }))
                    .build())
            .build()
            .start();
    var builder =
        InProcessChannelBuilder.forName(serverName)
            .directExecutor()
            .intercept(new GrpcCircuitBreakerInterceptor(breaker));
    if (retry) {
      builder
          .enableRetry()
          .defaultServiceConfig(
              resilience.retryConfig("account", AccountRpcServiceGrpc.getServiceDescriptor()));
    } else {
      builder.disableRetry();
    }
    channel = builder.build();
    return method;
  }

  private void invoke(MethodDescriptor<Empty, Empty> method) {
    ClientCalls.blockingUnaryCall(
        channel,
        method,
        CallOptions.DEFAULT.withDeadlineAfter(2, TimeUnit.SECONDS),
        Empty.getDefaultInstance());
  }

  @AfterEach
  void close() throws Exception {
    if (channel != null) {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (server != null) {
      server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
    meters.close();
  }

  @Test
  void opensRejectsLimitsProbesAndRecoversWithoutRestart() throws Exception {
    var method = start("GetAccountHealth", false);
    response = Status.UNAVAILABLE;
    for (int i = 0; i < 2; i++) {
      assertThatThrownBy(() -> invoke(method)).isInstanceOf(StatusRuntimeException.class);
    }
    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    assertThatThrownBy(() -> invoke(method)).hasMessageContaining("Downstream circuit open");
    assertThat(requests.get()).isEqualTo(2);
    Thread.sleep(180);
    hold = true;
    var first =
        ClientCalls.futureUnaryCall(
            channel.newCall(method, CallOptions.DEFAULT), Empty.getDefaultInstance());
    var second =
        ClientCalls.futureUnaryCall(
            channel.newCall(method, CallOptions.DEFAULT), Empty.getDefaultInstance());
    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
    assertThatThrownBy(() -> invoke(method)).hasMessageContaining("Downstream circuit open");
    assertThat(requests.get()).isEqualTo(4);
    for (var observer : pending) {
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
    }
    first.get(1, TimeUnit.SECONDS);
    second.get(1, TimeUnit.SECONDS);
    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    hold = false;
    response = Status.OK;
    invoke(method);
    assertThat(requests.get()).isEqualTo(5);
    assertThat(resilience.breaker("account")).isSameAs(breaker);
    assertThat(resilience.breaker("card").getMetrics().getNumberOfBufferedCalls()).isZero();
    assertThat(
            meters
                .get("resilience4j.circuitbreaker.not.permitted.calls")
                .tag("name", "account")
                .counter()
                .count())
        .isEqualTo(2);
  }

  @ParameterizedTest
  @EnumSource(
      value = Status.Code.class,
      names = {
        "INVALID_ARGUMENT",
        "NOT_FOUND",
        "PERMISSION_DENIED",
        "UNAUTHENTICATED",
        "FAILED_PRECONDITION",
        "RESOURCE_EXHAUSTED",
        "CANCELLED"
      })
  void ignoresNonTechnicalStatuses(Status.Code code) throws Exception {
    var method = start("GetAccountHealth", true);
    response = Status.fromCode(code);
    for (int i = 0; i < 3; i++) {
      assertThatThrownBy(() -> invoke(method)).isInstanceOf(StatusRuntimeException.class);
    }
    assertThat(requests.get()).isEqualTo(3);
    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    assertThat(breaker.getMetrics().getNumberOfBufferedCalls()).isZero();
  }

  @ParameterizedTest
  @EnumSource(
      value = Status.Code.class,
      names = {"UNAVAILABLE", "DEADLINE_EXCEEDED", "INTERNAL"})
  void recordsTechnicalStatuses(Status.Code code) throws Exception {
    var method = start("GetAccountHealth", false);
    response = Status.fromCode(code);
    for (int i = 0; i < 2; i++) {
      assertThatThrownBy(() -> invoke(method)).isInstanceOf(StatusRuntimeException.class);
    }
    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(2);
  }

  @Test
  void retriesReadsAndRecordsOneLogicalFailure() throws Exception {
    var method = start("GetAccountHealth", true);
    response = Status.UNAVAILABLE;
    assertThatThrownBy(() -> invoke(method)).isInstanceOf(StatusRuntimeException.class);
    assertThat(requests.get()).isEqualTo(2);
    assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
  }

  @Test
  void doesNotRetryWrites() throws Exception {
    var method = start("TopUpAccount", true);
    response = Status.UNAVAILABLE;
    assertThatThrownBy(() -> invoke(method)).isInstanceOf(StatusRuntimeException.class);
    assertThat(requests.get()).isEqualTo(1);
  }

  @Test
  void honorsDeadlineAcrossAttempts() throws Exception {
    var method = start("GetAccountHealth", true);
    hold = true;
    var call =
        channel.newCall(method, CallOptions.DEFAULT.withDeadlineAfter(50, TimeUnit.MILLISECONDS));
    var result = ClientCalls.futureUnaryCall(call, Empty.getDefaultInstance());
    assertThatThrownBy(() -> result.get(1, TimeUnit.SECONDS))
        .hasCauseInstanceOf(StatusRuntimeException.class);
    assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
    assertThat(requests.get()).isEqualTo(1);
  }

  @Test
  void canEnableResourceExhaustedAndOverrideDependencySettings() {
    environment
        .withProperty("grpc.resilience.instances.card.record-resource-exhausted", "true")
        .withProperty("grpc.resilience.instances.card.minimum-calls", "1");
    var card = resilience.breaker("card");
    card.onError(1, TimeUnit.MILLISECONDS, Status.RESOURCE_EXHAUSTED.asRuntimeException());
    assertThat(card.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
  }

  @Test
  void retryCanRecoverTransientFailure() throws Exception {
    var method = start("GetAccountHealth", true);
    response = Status.UNAVAILABLE;
    recoverOnRetry = true;
    invoke(method);
    assertThat(requests.get()).isEqualTo(2);
    assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isZero();
    assertThat(breaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
  }

  @Test
  void halfOpenFailureReopensCircuit() throws Exception {
    var method = start("GetAccountHealth", false);
    breaker.transitionToOpenState();
    breaker.transitionToHalfOpenState();
    response = Status.INTERNAL;
    for (int i = 0; i < 2; i++) {
      assertThatThrownBy(() -> invoke(method)).isInstanceOf(StatusRuntimeException.class);
    }
    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
  }

  @Test
  void slowSuccessfulCallsOpenCircuit() {
    for (int i = 0; i < 2; i++) {
      breaker.acquirePermission();
      breaker.onSuccess(1600, TimeUnit.MILLISECONDS);
    }
    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isZero();
  }

  @Test
  void cancellationBeforeStartDoesNotSendRequestOrConsumeProbe() throws Exception {
    var method = start("GetAccountHealth", false);
    breaker.transitionToOpenState();
    breaker.transitionToHalfOpenState();
    var call = channel.newCall(method, CallOptions.DEFAULT);
    call.cancel("Caller cancelled", null);
    var result = ClientCalls.futureUnaryCall(call, Empty.getDefaultInstance());
    assertThatThrownBy(() -> result.get(1, TimeUnit.SECONDS))
        .hasCauseInstanceOf(StatusRuntimeException.class);
    assertThat(requests.get()).isZero();
    invoke(method);
    invoke(method);
    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
  }

  @Test
  void streamFirstMessageReleasesProbeAndCancellationDoesNotCountTwice() {
    var method = method("WatchTransactionStatus", MethodDescriptor.MethodType.SERVER_STREAMING);
    var transport = org.mockito.Mockito.mock(io.grpc.Channel.class);
    @SuppressWarnings("unchecked")
    ClientCall<Empty, Empty> delegate = org.mockito.Mockito.mock(ClientCall.class);
    org.mockito.Mockito.when(transport.newCall(method, CallOptions.DEFAULT)).thenReturn(delegate);
    var interceptor = new GrpcCircuitBreakerInterceptor(breaker);
    var call = interceptor.interceptCall(method, CallOptions.DEFAULT, transport);
    call.start(new ClientCall.Listener<>() {}, new Metadata());
    @SuppressWarnings("unchecked")
    org.mockito.ArgumentCaptor<ClientCall.Listener<Empty>> captor =
        org.mockito.ArgumentCaptor.forClass(ClientCall.Listener.class);
    org.mockito.Mockito.verify(delegate)
        .start(captor.capture(), org.mockito.ArgumentMatchers.any());
    captor.getValue().onMessage(Empty.getDefaultInstance());
    captor.getValue().onMessage(Empty.getDefaultInstance());
    captor.getValue().onClose(Status.CANCELLED, new Metadata());
    assertThat(breaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
    assertThat(breaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
  }

  @Test
  void exportsStateTransitionsFailedAndRejectedCalls() throws Exception {
    channel =
        resilience.channel("account", "localhost", 1, AccountRpcServiceGrpc.getServiceDescriptor());
    breaker.onError(1, TimeUnit.MILLISECONDS, Status.INTERNAL.asRuntimeException());
    breaker.onError(1, TimeUnit.MILLISECONDS, Status.INTERNAL.asRuntimeException());
    assertThat(breaker.tryAcquirePermission()).isFalse();
    assertThat(
            meters
                .get("grpc.client.circuit.transitions")
                .tag("dependency", "account")
                .tag("transition", "CLOSED_TO_OPEN")
                .counter()
                .count())
        .isEqualTo(1);
    assertThat(
            meters
                .get("resilience4j.circuitbreaker.state")
                .tag("name", "account")
                .tag("state", "open")
                .gauge()
                .value())
        .isEqualTo(1);
    assertThat(
            meters
                .get("resilience4j.circuitbreaker.calls")
                .tag("name", "account")
                .tag("kind", "failed")
                .timer()
                .count())
        .isEqualTo(2);
    assertThat(
            meters
                .get("resilience4j.circuitbreaker.not.permitted.calls")
                .tag("name", "account")
                .counter()
                .count())
        .isEqualTo(1);
  }
}
