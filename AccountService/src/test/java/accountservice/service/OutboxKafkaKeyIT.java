package accountservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import accountservice.entity.AccountOutboxEventEntity;
import accountservice.mapper.eventpayload.AccountEventPayloadMapper;
import accountservice.repository.AccountOutboxEventRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import kafkacontracts.account.AccountEventType;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import outboxsupport.OutboxEventStatus;

@Tag("integration")
class OutboxKafkaKeyIT {
  @Test
  void publisherPreservesOrderPerAggregateAndDistributesAggregates() throws Exception {
    String bootstrap = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:29092");
    String topic = "outbox-key-test-" + UUID.randomUUID();
    Map<String, Object> producerConfig =
        Map.of("bootstrap.servers", bootstrap, "acks", "all", "enable.idempotence", true);
    var producerFactory =
        new DefaultKafkaProducerFactory<String, SpecificRecord>(
            producerConfig,
            new StringSerializer(),
            (ignored, payload) -> payload.toString().getBytes(StandardCharsets.UTF_8));
    try (Admin admin = Admin.create(Map.of("bootstrap.servers", bootstrap))) {
      admin
          .createTopics(List.of(new NewTopic(topic, 4, (short) 1)))
          .all()
          .get(30, TimeUnit.SECONDS);
      try (KafkaConsumer<String, String> consumer =
          new KafkaConsumer<>(
              Map.of(
                  "bootstrap.servers", bootstrap, "group.id", topic, "enable.auto.commit", false),
              new StringDeserializer(),
              new StringDeserializer())) {
        var partitions = new ArrayList<TopicPartition>();
        for (int i = 0; i < 4; i++) {
          partitions.add(new TopicPartition(topic, i));
        }
        consumer.assign(partitions);
        consumer.seekToBeginning(partitions);
        var repository = mock(AccountOutboxEventRepository.class);
        var events = new ArrayList<AccountOutboxEventEntity>();
        var expectedIds = new LinkedHashMap<String, List<String>>();
        // Two directional completion events and compensation share the saga key.
        for (int aggregate = 0; aggregate < 16; aggregate++) {
          UUID id = new UUID(0, aggregate + 1);
          var ids = new ArrayList<String>();
          for (int sequence = 0; sequence < 3; sequence++) {
            var type =
                sequence == 2
                    ? AccountEventType.TRANSACTION_COMPENSATED
                    : AccountEventType.TRANSACTION_COMPLETED;
            var event = AccountOutboxEventFactory.create(id, type);
            event.setId(UUID.randomUUID());
            event.setTopic(topic); // Isolate the test; ordering is scoped to one topic.
            event.setPayload(
                Map.of(
                    "transactionId",
                    id,
                    "accountNumber",
                    "account-" + sequence,
                    "authUserId",
                    id,
                    "amountMinorUnits",
                    100L,
                    "currency",
                    "USD",
                    "transactionDirection",
                    sequence == 0 ? "RECIPIENT" : "SENDER"));
            events.add(event);
            ids.add(event.getId().toString());
            when(repository.findById(event.getId())).thenReturn(Optional.of(event));
          }
          expectedIds.put(id.toString(), ids);
        }
        when(repository.findTop50ByOutboxEventStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING))
            .thenReturn(events);
        var template = new KafkaTemplate<>(producerFactory);
        new AccountOutboxPublisher(repository, template, new AccountEventPayloadMapper() {})
            .publishPendingEvents();
        template.flush();
        var received = new ArrayList<ConsumerRecord<String, String>>();
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (received.size() < events.size() && System.nanoTime() < deadline) {
          consumer.poll(Duration.ofMillis(250)).forEach(received::add);
        }
        assertThat(received).hasSize(events.size());
        assertThat(received.stream().map(ConsumerRecord::partition).distinct().count())
            .isGreaterThan(1);
        for (var entry : expectedIds.entrySet()) {
          var aggregateRecords =
              received.stream().filter(r -> r.key().equals(entry.getKey())).toList();
          assertThat(aggregateRecords).hasSize(3);
          assertThat(aggregateRecords.stream().map(ConsumerRecord::partition).distinct().count())
              .isEqualTo(1);
          assertThat(
                  aggregateRecords.stream()
                      .map(
                          r ->
                              new String(
                                  r.headers().lastHeader("eventId").value(),
                                  StandardCharsets.UTF_8))
                      .toList())
              .containsExactlyElementsOf(entry.getValue());
          assertThat(aggregateRecords.stream().map(ConsumerRecord::offset).toList()).isSorted();
        }
        assertThat(
                received.stream()
                    .map(
                        r ->
                            new String(
                                r.headers().lastHeader("eventId").value(), StandardCharsets.UTF_8))
                    .distinct()
                    .count())
            .isEqualTo(events.size());
      } finally {
        producerFactory.destroy();
        admin.deleteTopics(List.of(topic)).all().get(30, TimeUnit.SECONDS);
      }
    }
  }
}
