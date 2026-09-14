package transactionservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.ServiceDescriptor;
import io.grpc.Status;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class GrpcResilience {
  private static final Logger log = LoggerFactory.getLogger(GrpcResilience.class);
  // Explicit read allowlist: new RPCs are never retried implicitly.
  private static final Set<String> RETRY_READS =
      Set.of(
          "GetAuthHealth",
          "GetAuthUserById",
          "GetAuthUserByIds",
          "GetUserHealth",
          "GetUserInfo",
          "GetAllUserInfo",
          "GetRecipientByEmail",
          "GetAccountHealth",
          "GetAccountsByOwnerUserId",
          "GetAccountsByAuthUserId",
          "GetRecipientAccountsByOwnerUserId",
          "GetAllAccounts",
          "GetAccountById",
          "GetAccountByIdsForTransaction",
          "GetCardHealth",
          "GetCardsByAccountId",
          "GetCardsByAccountIds",
          "GetTransactionHealth",
          "GetTransactionsByAccounts",
          "GetNotificationHealth");
  private final Environment environment;
  private final MeterRegistry meters;
  private final CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();

  public GrpcResilience(Environment environment, MeterRegistry meters) {
    this.environment = environment;
    this.meters = meters;
    TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meters);
  }

  public CircuitBreaker breaker(String dependency) {
    return registry.circuitBreaker(dependency, () -> config(dependency));
  }

  private CircuitBreakerConfig config(String dependency) {
    return CircuitBreakerConfig.custom()
        .slidingWindowType(
            value(
                dependency,
                "window-type",
                CircuitBreakerConfig.SlidingWindowType.class,
                CircuitBreakerConfig.SlidingWindowType.COUNT_BASED))
        .slidingWindowSize(value(dependency, "window-size", Integer.class, 20))
        .minimumNumberOfCalls(value(dependency, "minimum-calls", Integer.class, 10))
        .failureRateThreshold(value(dependency, "failure-rate-threshold", Float.class, 50f))
        .slowCallRateThreshold(value(dependency, "slow-call-rate-threshold", Float.class, 50f))
        .slowCallDurationThreshold(
            Duration.ofMillis(value(dependency, "slow-call-duration-ms", Long.class, 1500L)))
        .waitDurationInOpenState(
            Duration.ofMillis(value(dependency, "open-wait-ms", Long.class, 10000L)))
        .permittedNumberOfCallsInHalfOpenState(
            value(dependency, "half-open-calls", Integer.class, 3))
        .maxWaitDurationInHalfOpenState(
            Duration.ofMillis(value(dependency, "half-open-max-wait-ms", Long.class, 5000L)))
        .ignoreException(error -> !technical(Status.fromThrowable(error).getCode(), dependency))
        .build();
  }

  private boolean technical(Status.Code code, String dependency) {
    return switch (code) {
      case UNAVAILABLE, DEADLINE_EXCEEDED, INTERNAL -> true;
      case RESOURCE_EXHAUSTED ->
          value(dependency, "record-resource-exhausted", Boolean.class, false);
      default -> false;
    };
  }

  public ManagedChannel channel(
      String dependency, String host, int port, ServiceDescriptor service) {
    CircuitBreaker circuit = breaker(dependency);
    circuit
        .getEventPublisher()
        .onStateTransition(
            event -> {
              log.info(
                  "grpc_circuit_transition dependency={} transition={}",
                  dependency,
                  event.getStateTransition());
              meters
                  .counter(
                      "grpc.client.circuit.transitions",
                      "dependency",
                      dependency,
                      "transition",
                      event.getStateTransition().name())
                  .increment();
            })
        .onCallNotPermitted(
            event ->
                log.warn(
                    "grpc_circuit_rejected dependency={} state={}",
                    dependency,
                    circuit.getState()));
    return ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext()
        .disableServiceConfigLookUp()
        .defaultServiceConfig(retryConfig(dependency, service))
        .enableRetry()
        .intercept(new GrpcCircuitBreakerInterceptor(circuit))
        .build();
  }

  Map<String, ?> retryConfig(String dependency, ServiceDescriptor service) {
    int attempts = value(dependency, "retry.max-attempts", Integer.class, 2);
    long initial = value(dependency, "retry.initial-backoff-ms", Long.class, 100L);
    long maximum = value(dependency, "retry.max-backoff-ms", Long.class, 500L);
    double multiplier = value(dependency, "retry.backoff-multiplier", Double.class, 2d);
    if (attempts < 1 || attempts > 5 || initial <= 0 || maximum < initial || multiplier < 1) {
      throw new IllegalArgumentException("Invalid gRPC retry configuration for " + dependency);
    }
    var names =
        service.getMethods().stream()
            .filter(method -> method.getType() == MethodDescriptor.MethodType.UNARY)
            .filter(method -> RETRY_READS.contains(method.getBareMethodName()))
            .map(
                method ->
                    Map.of("service", service.getName(), "method", method.getBareMethodName()))
            .toList();
    if (attempts == 1 || names.isEmpty()) {
      return Map.of();
    }
    return Map.of(
        "methodConfig",
        List.of(
            Map.of(
                "name",
                names,
                "retryPolicy",
                Map.of(
                    "maxAttempts",
                    (double) attempts,
                    "initialBackoff",
                    initial / 1000.0 + "s",
                    "maxBackoff",
                    maximum / 1000.0 + "s",
                    "backoffMultiplier",
                    multiplier,
                    "retryableStatusCodes",
                    List.of("UNAVAILABLE")))));
  }

  private <T> T value(String dependency, String key, Class<T> type, T fallback) {
    return environment.getProperty(
        "grpc.resilience.instances." + dependency + "." + key,
        type,
        environment.getProperty("grpc.resilience.defaults." + key, type, fallback));
  }
}
