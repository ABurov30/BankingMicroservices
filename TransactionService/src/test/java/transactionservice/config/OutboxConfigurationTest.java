package transactionservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;
import org.apache.avro.specific.SpecificRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.env.MockEnvironment;

class OutboxConfigurationTest {
  private final OutboxConfiguration configuration = new OutboxConfiguration();
  private final KafkaTemplate<String, SpecificRecord> kafka =
      new KafkaTemplate<>(
          new DefaultKafkaProducerFactory<>(
              Map.of("max.block.ms", 5000, "delivery.timeout.ms", 30000)));

  @Test
  void usesConservativeDefaultsAndBindsAllPublicSettingsInMilliseconds() {
    var defaults = configuration.outboxProperties(new MockEnvironment(), kafka);
    assertThat(defaults.polling()).isEqualTo(Duration.ofSeconds(5));
    var environment =
        new MockEnvironment()
            .withProperty("banking.outbox.poll-interval-ms", "500")
            .withProperty("banking.outbox.initial-delay-ms", "0")
            .withProperty("banking.outbox.batch-size", "3")
            .withProperty("banking.outbox.max-in-flight", "2")
            .withProperty("banking.outbox.lease-timeout-ms", "60000")
            .withProperty("banking.outbox.retry-delay-ms", "250")
            .withProperty("banking.outbox.max-attempts", "7");
    var settings = configuration.outboxProperties(environment, kafka);
    assertThat(settings.polling()).isEqualTo(Duration.ofMillis(500));
    assertThat(settings.initialDelay()).isEqualTo(Duration.ZERO);
    assertThat(settings.batchSize()).isEqualTo(3);
    assertThat(settings.maxInFlight()).isEqualTo(2);
    assertThat(settings.lease()).isEqualTo(Duration.ofMinutes(1));
    assertThat(settings.retryDelay()).isEqualTo(Duration.ofMillis(250));
    assertThat(settings.maxAttempts()).isEqualTo(7);
  }

  @ParameterizedTest
  @CsvSource({
    "poll-interval-ms,0",
    "initial-delay-ms,-1",
    "batch-size,0",
    "max-in-flight,0",
    "lease-timeout-ms,0",
    "retry-delay-ms,0",
    "max-attempts,0"
  })
  void rejectsInvalidValuesAtStartup(String name, String value) {
    assertThatThrownBy(
            () ->
                configuration.outboxProperties(
                    new MockEnvironment().withProperty("banking.outbox." + name, value), kafka))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsLeaseThatCouldExpireBeforeBatchSendAndAcknowledgement() {
    var environment =
        new MockEnvironment().withProperty("banking.outbox.lease-timeout-ms", "285000");
    assertThatThrownBy(() -> configuration.outboxProperties(environment, kafka))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Kafka max.block.ms");
  }
}
