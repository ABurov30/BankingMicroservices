package accountservice.config;

import accountservice.entity.AccountOutboxEventEntity;
import accountservice.repository.AccountOutboxEventRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.Map;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import outboxsupport.JpaOutboxAttemptStore;
import outboxsupport.OutboxDispatcher;
import outboxsupport.OutboxEventStatus;
import outboxsupport.OutboxProperties;

@Configuration(proxyBeanMethods = false)
public class OutboxConfiguration {
  @Bean
  public OutboxProperties outboxProperties(
      Environment environment, KafkaTemplate<String, SpecificRecord> kafkaTemplate) {
    var properties =
        new OutboxProperties(
            duration(environment, "poll-interval-ms", 5000),
            duration(environment, "initial-delay-ms", 5000),
            environment.getProperty("banking.outbox.batch-size", Integer.class, 50),
            environment.getProperty("banking.outbox.max-in-flight", Integer.class, 50),
            duration(environment, "lease-timeout-ms", 360000),
            duration(environment, "retry-delay-ms", 5000),
            environment.getProperty("banking.outbox.max-attempts", Integer.class, 5));
    Map<String, Object> producer = kafkaTemplate.getProducerFactory().getConfigurationProperties();
    long blocking =
        Long.parseLong(producer.getOrDefault(ProducerConfig.MAX_BLOCK_MS_CONFIG, 60000).toString());
    long delivery =
        Long.parseLong(
            producer.getOrDefault(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000).toString());
    long batchBlocking =
        Math.multiplyExact(Math.min(properties.batchSize(), properties.maxInFlight()), blocking);
    long minimumLease = Math.addExact(Math.addExact(batchBlocking, delivery), 5000);
    if (blocking < 0 || delivery < 1 || properties.lease().toMillis() <= minimumLease) {
      throw new IllegalArgumentException(
          "banking.outbox.lease-timeout-ms must exceed "
              + "min(batch-size, max-in-flight) * Kafka max.block.ms + delivery.timeout.ms + 5000");
    }
    return properties;
  }

  @Bean
  public OutboxDispatcher<AccountOutboxEventEntity> outboxDispatcher(
      EntityManager entityManager,
      PlatformTransactionManager transactionManager,
      OutboxProperties properties,
      MeterRegistry meters,
      AccountOutboxEventRepository repository) {
    Gauge.builder(
            "outbox.pending",
            repository,
            r -> r.countByOutboxEventStatus(OutboxEventStatus.PENDING))
        .tag("outbox", "account_outbox_events")
        .register(meters);
    Gauge.builder(
            "outbox.processing",
            repository,
            r -> r.countByOutboxEventStatus(OutboxEventStatus.PROCESSING))
        .tag("outbox", "account_outbox_events")
        .register(meters);
    meters.counter("outbox.retry", "outbox", "account_outbox_events");
    var store =
        new JpaOutboxAttemptStore<>(
            entityManager,
            transactionManager,
            AccountOutboxEventEntity.class,
            "account_outbox_events",
            properties);
    return new OutboxDispatcher<>(
        store, properties, transactionManager, meters, "account_outbox_events");
  }

  private static Duration duration(Environment environment, String name, long defaultValue) {
    return Duration.ofMillis(
        environment.getProperty("banking.outbox." + name, Long.class, defaultValue));
  }
}
