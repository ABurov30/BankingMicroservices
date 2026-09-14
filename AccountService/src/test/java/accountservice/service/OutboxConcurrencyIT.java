package accountservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import accountservice.entity.AccountOutboxEventEntity;
import accountservice.mapper.eventpayload.AccountEventPayloadMapper;
import accountservice.repository.AccountOutboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import kafkacontracts.account.AccountEventType;
import org.apache.avro.specific.SpecificRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import outboxsupport.JpaOutboxAttemptStore;
import outboxsupport.OutboxDispatcher;
import outboxsupport.OutboxEventStatus;
import outboxsupport.OutboxProperties;

@Tag("integration")
@ActiveProfiles("test")
@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate", showSql = false)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OutboxConcurrencyIT {
  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

  @Autowired private EntityManager entityManager;
  @Autowired private PlatformTransactionManager manager;
  @Autowired private AccountOutboxEventRepository repository;

  private final OutboxProperties properties =
      new OutboxProperties(
          Duration.ofMillis(500),
          Duration.ZERO,
          10,
          2,
          Duration.ofSeconds(10),
          Duration.ofSeconds(5),
          2);

  @BeforeEach
  void clean() {
    repository.deleteAllInBatch();
  }

  @Test
  void twoPublishersCannotSendTheSamePendingRecordWhileAckIsOutstanding() throws Exception {
    var event = create();
    var ack = new CompletableFuture<SendResult<String, SpecificRecord>>();
    @SuppressWarnings("unchecked")
    KafkaTemplate<String, SpecificRecord> kafka = mock(KafkaTemplate.class);
    AtomicInteger sends = new AtomicInteger();
    when(kafka.send(any(Message.class)))
        .thenAnswer(
            invocation -> {
              assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
              sends.incrementAndGet();
              return ack;
            });
    var meters1 = new SimpleMeterRegistry();
    var meters2 = new SimpleMeterRegistry();
    var first = publisher(kafka, meters1);
    var second = publisher(kafka, meters2);
    var executor = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    try {
      var a =
          executor.submit(
              () -> {
                await(start);
                first.publishPendingEvents();
              });
      var b =
          executor.submit(
              () -> {
                await(start);
                second.publishPendingEvents();
              });
      start.countDown();
      a.get(10, TimeUnit.SECONDS);
      b.get(10, TimeUnit.SECONDS);
      first.publishPendingEvents();
      second.publishPendingEvents();
      assertThat(sends).hasValue(1);
      assertThat(repository.findById(event.getId()).orElseThrow().getOutboxEventStatus())
          .isEqualTo(OutboxEventStatus.PROCESSING);
      ack.complete(null);
      var published = repository.findById(event.getId()).orElseThrow();
      assertThat(published.getOutboxEventStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
      assertThat(published.getSentAt()).isNotNull();
      assertThat(published.getLockedBy()).isNull();
      assertThat(published.getRetryCount()).isEqualTo(1);
      assertThat(meters1.get("outbox.in.flight").gauge().value()).isZero();
      assertThat(meters2.get("outbox.in.flight").gauge().value()).isZero();
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void concurrentBatchesAreDisjoint() throws Exception {
    for (int i = 0; i < 20; i++) {
      create();
    }
    var executor = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    try {
      var a =
          executor.submit(
              () -> {
                await(start);
                return store().claim(10);
              });
      var b =
          executor.submit(
              () -> {
                await(start);
                return store().claim(10);
              });
      start.countDown();
      var first = a.get(10, TimeUnit.SECONDS);
      var second = b.get(10, TimeUnit.SECONDS);
      assertThat(first).hasSize(10);
      assertThat(second).hasSize(10);
      assertThat(first.stream().map(attempt -> attempt.eventId()).toList())
          .doesNotContainAnyElementsOf(second.stream().map(attempt -> attempt.eventId()).toList());
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void lockedRowsAreSkippedWithoutWaitingForTheirTransaction() throws Exception {
    var locked = create();
    var available = create();
    var acquired = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);
    var locking =
        executor.submit(
            () ->
                new TransactionTemplate(manager)
                    .executeWithoutResult(
                        status -> {
                          entityManager
                              .createNativeQuery(
                                  "SELECT id FROM account_outbox_events WHERE id = :id FOR UPDATE")
                              .setParameter("id", locked.getId())
                              .getResultList();
                          acquired.countDown();
                          await(release);
                        }));
    try {
      assertThat(acquired.await(10, TimeUnit.SECONDS)).isTrue();
      var claimed = executor.submit(() -> store().claim(10)).get(5, TimeUnit.SECONDS);
      assertThat(claimed).hasSize(1);
      assertThat(claimed.get(0).eventId()).isEqualTo(available.getId());
    } finally {
      release.countDown();
      locking.get(10, TimeUnit.SECONDS);
      executor.shutdownNow();
    }
  }

  @Test
  void leaseRecoveryRespectsRetryDelayAndFencesOldCallbacks() {
    create();
    var store = store();
    var old = store.claim(1).get(0);
    assertThat(repository.ownsAttempt(old.eventId(), old.token(), 10000)).isTrue();
    expire(old.eventId());
    assertThat(repository.ownsAttempt(old.eventId(), old.token(), 10000)).isFalse();
    assertThat(store.recover()).isEqualTo(1);
    assertThat(store.claim(1)).isEmpty();
    ready(old.eventId());
    var next = store.claim(1).get(0);
    assertThat(next.token()).isNotEqualTo(old.token());
    assertThat(store.complete(old, null)).isFalse();
    assertThat(store.complete(old, new RuntimeException("late failure"))).isFalse();
    assertThat(store.complete(next, null)).isTrue();
    assertThat(store.complete(next, new RuntimeException("duplicate callback"))).isFalse();
    assertThat(repository.findById(next.eventId()).orElseThrow().getOutboxEventStatus())
        .isEqualTo(OutboxEventStatus.PUBLISHED);
  }

  @Test
  void errorsRetryUntilAttemptLimitAndExpiredLastAttemptsFail() {
    create();
    var store = store();
    var first = store.claim(1).get(0);
    assertThat(store.complete(first, new RuntimeException("Kafka down"))).isTrue();
    assertThat(store.claim(1)).isEmpty();
    ready(first.eventId());
    var last = store.claim(1).get(0);
    assertThat(store.complete(last, new RuntimeException("Kafka still down"))).isTrue();
    assertThat(repository.findById(last.eventId()).orElseThrow().getOutboxEventStatus())
        .isEqualTo(OutboxEventStatus.FAILED);
    create();
    var abandoned = store.claim(1).get(0);
    expire(abandoned.eventId());
    store.recover();
    ready(abandoned.eventId());
    var abandonedLast = store.claim(1).get(0);
    expire(abandonedLast.eventId());
    store.recover();
    assertThat(repository.findById(abandonedLast.eventId()).orElseThrow().getOutboxEventStatus())
        .isEqualTo(OutboxEventStatus.FAILED);
    assertThat(store.claim(10)).isEmpty();
  }

  @Test
  void dispatcherCapsInFlightAndSuspendsCallerTransaction() {
    for (int i = 0; i < 4; i++) {
      create();
    }
    var dispatcher =
        new OutboxDispatcher<>(store(), properties, manager, new SimpleMeterRegistry(), "test");
    List<CompletableFuture<Void>> acks = new ArrayList<>();
    new TransactionTemplate(manager)
        .executeWithoutResult(
            status -> {
              dispatcher.dispatch(
                  event -> {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                        .isFalse();
                    assertThat(entityManager.contains(event)).isFalse();
                    var ack = new CompletableFuture<Void>();
                    acks.add(ack);
                    return ack;
                  });
              assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            });
    assertThat(acks).hasSize(2);
    dispatcher.dispatch(
        event -> {
          throw new AssertionError("in-flight limit exceeded");
        });
    acks.get(0).complete(null);
    dispatcher.dispatch(
        event -> {
          acks.add(new CompletableFuture<>());
          return acks.get(2);
        });
    assertThat(acks).hasSize(3);
    acks.forEach(ack -> ack.complete(null));
  }

  @Test
  void publisherDoesNotSendIfLeaseExpiresDuringPayloadMapping() {
    var event = create();
    @SuppressWarnings("unchecked")
    KafkaTemplate<String, SpecificRecord> kafka = mock(KafkaTemplate.class);
    var mapper =
        new AccountEventPayloadMapper() {
          @Override
          public kafkacontracts.account.AccountCreatedEventPayload toAccountCreatedEventPayload(
              Map<String, Object> payload) {
            expire(event.getId());
            return AccountEventPayloadMapper.super.toAccountCreatedEventPayload(payload);
          }
        };
    var dispatcher =
        new OutboxDispatcher<>(store(), properties, manager, new SimpleMeterRegistry(), "test");
    new AccountOutboxPublisher(
            dispatcher, kafka, mapper, repository, properties, new SimpleMeterRegistry())
        .publishPendingEvents();
    verifyNoInteractions(kafka);
  }

  private AccountOutboxPublisher publisher(
      KafkaTemplate<String, SpecificRecord> kafka, SimpleMeterRegistry meters) {
    return new AccountOutboxPublisher(
        new OutboxDispatcher<>(store(), properties, manager, meters, "account_outbox_events"),
        kafka,
        new AccountEventPayloadMapper() {},
        repository,
        properties,
        meters);
  }

  private JpaOutboxAttemptStore<AccountOutboxEventEntity> store() {
    return new JpaOutboxAttemptStore<>(
        entityManager,
        manager,
        AccountOutboxEventEntity.class,
        "account_outbox_events",
        properties);
  }

  private AccountOutboxEventEntity create() {
    UUID id = UUID.randomUUID();
    var event = AccountOutboxEventFactory.create(id, AccountEventType.ACCOUNT_CREATED);
    event.setPayload(
        Map.of(
            "accountId", id, "authUserId", id, "accountNumber", "test-account", "currency", "USD"));
    return repository.saveAndFlush(event);
  }

  private void expire(UUID id) {
    update("locked_at = CURRENT_TIMESTAMP - INTERVAL '1 hour'", id);
  }

  private void ready(UUID id) {
    update("next_retry_at = CURRENT_TIMESTAMP - INTERVAL '1 second'", id);
  }

  private void update(String assignment, UUID id) {
    new TransactionTemplate(manager)
        .executeWithoutResult(
            status ->
                entityManager
                    .createNativeQuery(
                        "UPDATE account_outbox_events SET " + assignment + " WHERE id = :id")
                    .setParameter("id", id)
                    .executeUpdate());
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new AssertionError("Latch timed out");
      }
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new AssertionError(error);
    }
  }
}
