package cardservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import cardservice.dto.CompensateLimitsForTransactionCommand;
import cardservice.dto.ReserveLimitsForTransactionCommand;
import cardservice.dto.ReserveLimitsForTransactionResult;
import cardservice.entity.AccountOwnershipProjectionEntity;
import cardservice.entity.CardEntity;
import cardservice.mapper.result.CardResultMapper;
import cardservice.repository.AccountOwnershipProjectionRepository;
import cardservice.repository.CardLimitHoldRepository;
import cardservice.repository.CardRepository;
import enums.account.ReservationStatus;
import enums.common.Currency;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Tag("integration")
@ActiveProfiles("test")
@Testcontainers
@DataJpaTest(
    properties = {
      "spring.config.name=card-limit-reservation-test",
      "spring.jpa.hibernate.ddl-auto=validate"
    })
@Import({
  CardService.class,
  CardLimitReservationService.class,
  CardLimitReservationIT.RaceConfiguration.class,
  AccountOverviewCacheInvalidationService.class,
  CardOutboxService.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CardLimitReservationIT {
  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

  @Autowired private CardService service;
  @Autowired private CardRepository cards;
  @Autowired private AccountOwnershipProjectionRepository projections;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private CardLimitHoldRepository holds;
  @Autowired private HoldCheckBarrier holdCheckBarrier;
  @MockitoBean private CardResultMapper mapper;

  private UUID cardId;
  private UUID ownerId;

  @BeforeEach
  void prepare() {
    holds.deleteAll();
    cards.deleteAll();
    projections.deleteAll();
    ownerId = UUID.randomUUID();
    var projection = new AccountOwnershipProjectionEntity();
    projection.setAccountId(UUID.randomUUID());
    projection.setOwnerAuthUserId(ownerId);
    projection.setCurrency(Currency.USD);
    projections.saveAndFlush(projection);

    var card = new CardEntity();
    card.setAccountId(projection.getAccountId());
    card.setPan("4000000000000002");
    card.setCurrency(Currency.USD);
    card.setExpiresAt(LocalDateTime.now().plusYears(1));
    card.setDailyLimitMinorUnits(100L);
    card.setMonthlyLimitMinorUnits(100L);
    cardId = cards.saveAndFlush(card).getId();
  }

  @AfterEach
  void removeFailureTrigger() {
    jdbc.execute("DROP TRIGGER IF EXISTS fail_card_commit ON cards");
    jdbc.execute("DROP FUNCTION IF EXISTS fail_card_commit()");
  }

  @Test
  void concurrentReservationsCannotExceedDailyLimit() throws Exception {
    var card = cards.findById(cardId).orElseThrow();
    card.setMonthlyLimitMinorUnits(1000L);
    cards.saveAndFlush(card);
    reserveConcurrently(command(UUID.randomUUID()), command(UUID.randomUUID()));
    assertReservationState(60L, 60L);
  }

  @Test
  void concurrentReservationsCannotExceedRemainingMonthlyLimit() throws Exception {
    // Previous days consumed 900 of the 1000 monthly limit; today's counter is zero.
    var card = cards.findById(cardId).orElseThrow();
    card.setDailyLimitMinorUnits(1000L);
    card.setMonthlyLimitMinorUnits(1000L);
    card.setSpendMonthlyLimitMinorUnits(900L);
    cards.saveAndFlush(card);
    reserveConcurrently(command(UUID.randomUUID()), command(UUID.randomUUID()));
    assertReservationState(60L, 960L);
  }

  @Test
  void concurrentDuplicateRollsBackAndReturnsFailed() throws Exception {
    var card = cards.findById(cardId).orElseThrow();
    card.setDailyLimitMinorUnits(1000L);
    card.setMonthlyLimitMinorUnits(1000L);
    cards.saveAndFlush(card);
    var command = command(UUID.randomUUID());
    reserveConcurrently(command, command);
    assertReservationState(60L, 60L);
    assertThat(holds.findByTransactionId(command.transactionId())).isPresent();
  }

  @Test
  void commitFailureRollsBackHoldAndBothCountersAndReturnsFailed() {
    // A deferred trigger fails only at commit, after INSERT and UPDATE have executed.
    jdbc.execute(
        """
        CREATE FUNCTION fail_card_commit() RETURNS trigger LANGUAGE plpgsql AS $$
        BEGIN
          RAISE EXCEPTION 'injected card commit failure';
        END;
        $$
        """);
    jdbc.execute(
        """
        CREATE CONSTRAINT TRIGGER fail_card_commit
        AFTER UPDATE ON cards DEFERRABLE INITIALLY DEFERRED
        FOR EACH ROW EXECUTE FUNCTION fail_card_commit()
        """);

    var result = service.reserveLimitsForTransaction(command(UUID.randomUUID()));

    assertThat(result.status()).isEqualTo(ReservationStatus.FAILED);
    assertThat(holds.count()).isZero();
    var card = cards.findById(cardId).orElseThrow();
    assertThat(card.getSpendDailyLimitMinorUnits()).isZero();
    assertThat(card.getSpendMonthlyLimitMinorUnits()).isZero();
  }

  @Test
  void compensationEventReleasesReservedHoldAndCardLimits() {
    var transactionId = UUID.randomUUID();
    assertThat(service.reserveLimitsForTransaction(command(transactionId)).status())
        .isEqualTo(ReservationStatus.RESERVED);
    service.compensateLimitsForTransaction(
        new CompensateLimitsForTransactionCommand(transactionId));

    var card = cards.findById(cardId).orElseThrow();
    assertThat(card.getSpendDailyLimitMinorUnits()).isZero();
    assertThat(card.getSpendMonthlyLimitMinorUnits()).isZero();
    assertThat(holds.findByTransactionId(transactionId))
        .get()
        .satisfies(
            hold -> {
              assertThat(hold.getStatus()).isEqualTo(ReservationStatus.COMPENSATED);
              assertThat(hold.getReleasedAt()).isNotNull();
            });
  }

  private ReserveLimitsForTransactionCommand command(UUID transactionId) {
    return new ReserveLimitsForTransactionCommand(
        cardId, 60L, transactionId, ownerId, Currency.USD);
  }

  private void reserveConcurrently(
      ReserveLimitsForTransactionCommand first, ReserveLimitsForTransactionCommand second)
      throws Exception {
    // Both transactions must observe no hold before either can acquire the card lock.
    holdCheckBarrier.barrier = new CyclicBarrier(2);
    var pool = Executors.newFixedThreadPool(2);
    try {
      var firstResult = pool.submit(() -> service.reserveLimitsForTransaction(first));
      var secondResult = pool.submit(() -> service.reserveLimitsForTransaction(second));
      ReserveLimitsForTransactionResult firstResponse = firstResult.get(20, TimeUnit.SECONDS);
      ReserveLimitsForTransactionResult secondResponse = secondResult.get(20, TimeUnit.SECONDS);
      assertThat(new ReservationStatus[] {firstResponse.status(), secondResponse.status()})
          .containsExactlyInAnyOrder(ReservationStatus.RESERVED, ReservationStatus.FAILED);
    } finally {
      pool.shutdownNow();
      assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
      holdCheckBarrier.barrier = null;
    }
  }

  private void assertReservationState(long dailySpend, long monthlySpend) {
    var card = cards.findById(cardId).orElseThrow();
    assertThat(card.getSpendDailyLimitMinorUnits()).isEqualTo(dailySpend);
    assertThat(card.getSpendMonthlyLimitMinorUnits()).isEqualTo(monthlySpend);
    assertThat(holds.findAll())
        .singleElement()
        .satisfies(
            hold -> {
              assertThat(hold.getCardId()).isEqualTo(cardId);
              assertThat(hold.getMinorUnits()).isEqualTo(60L);
              assertThat(hold.getStatus()).isEqualTo(ReservationStatus.RESERVED);
            });
  }

  @TestConfiguration(proxyBeanMethods = false)
  @EnableAspectJAutoProxy
  static class RaceConfiguration {
    @Bean
    HoldCheckBarrier holdCheckBarrier() {
      return new HoldCheckBarrier();
    }
  }

  @Aspect
  static class HoldCheckBarrier {
    private volatile CyclicBarrier barrier;

    @Around("execution(* cardservice.repository.CardLimitHoldRepository.existsByTransactionId(..))")
    public Object awaitBothChecks(ProceedingJoinPoint invocation) throws Throwable {
      Object result = invocation.proceed();
      CyclicBarrier current = barrier;
      if (current != null) {
        current.await(10, TimeUnit.SECONDS);
      }
      return result;
    }
  }
}
