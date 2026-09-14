package accountservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import accountservice.entity.AccountEntity;
import accountservice.repository.AccountHoldRepository;
import accountservice.repository.AccountInterestAccrualRepository;
import accountservice.repository.AccountRepository;
import accountservice.repository.CurrencyRepository;
import enums.account.AccountStatus;
import enums.account.AccountType;
import enums.common.Currency;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Tag("integration")
@ActiveProfiles("test")
@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Import(AccountInterestService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AccountInterestAccrualIT {
  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

  private static final LocalDate DATE = LocalDate.of(2026, 9, 14);
  @Autowired private AccountRepository accounts;
  @Autowired private AccountInterestAccrualRepository accruals;
  @Autowired private CurrencyRepository currencies;
  @Autowired private AccountHoldRepository holds;
  @Autowired private AccountInterestService interest;
  @Autowired private DataSource dataSource;
  @Autowired private PlatformTransactionManager transactionManager;
  private SimpleMeterRegistry metrics;
  private UUID accountId;

  @BeforeEach
  void prepare() {
    accruals.deleteAll();
    accounts.deleteAll();
    metrics = new SimpleMeterRegistry();
    accountId = createAccount(10000);
  }

  private UUID createAccount(long balance) {
    var account = new AccountEntity();
    account.setOwnerUserId(UUID.randomUUID());
    account.setOwnerAuthUserId(UUID.randomUUID());
    account.setAccountNumber(UUID.randomUUID().toString());
    account.setAccountType(AccountType.SAVINGS);
    account.setAccountStatus(AccountStatus.ACTIVE);
    account.setCurrency(currencies.findByName(Currency.USD));
    account.setAvailableBalanceMinorUnits(balance);
    return accounts.saveAndFlush(account).getId();
  }

  private AccountScheduler scheduler() {
    return new AccountScheduler(accounts, holds, interest, metrics, Clock.systemUTC());
  }

  private long balance() {
    return accounts.findById(accountId).orElseThrow().getAvailableBalanceMinorUnits();
  }

  @Test
  void firstRunAndRepeatedRunCreditExactlyOnce() {
    scheduler().updateAccountsBalances(DATE);
    scheduler().updateAccountsBalances(DATE);
    assertThat(balance()).isEqualTo(10002);
    assertThat(accruals.findAll())
        .singleElement()
        .satisfies(
            accrual -> {
              assertThat(accrual.getAccountId()).isEqualTo(accountId);
              assertThat(accrual.getAccrualDate()).isEqualTo(DATE);
              assertThat(accrual.getAmountMinorUnits()).isEqualTo(2);
              assertThat(accrual.getRate()).isEqualByComparingTo("7");
              assertThat(accrual.getCreatedAt()).isNotNull();
            });
    assertThat(
            metrics.get("account.interest.accruals").tag("result", "processed").counter().count())
        .isEqualTo(1);
    assertThat(metrics.get("account.interest.accruals").tag("result", "skipped").counter().count())
        .isEqualTo(1);
  }

  @Test
  void nextDateCreatesAnotherAccrual() {
    assertThat(interest.accrueInterest(accountId, DATE)).isTrue();
    assertThat(interest.accrueInterest(accountId, DATE.plusDays(1))).isTrue();
    assertThat(balance()).isEqualTo(10004);
    assertThat(accruals.findAll())
        .extracting("accrualDate")
        .containsExactlyInAnyOrder(DATE, DATE.plusDays(1));
  }

  @Test
  void twoSchedulerInstancesCreditEachAccountOnce() throws Exception {
    createAccount(10000);
    var first = scheduler();
    var second = scheduler();
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(2);
    try {
      var futures =
          List.of(first, second).stream()
              .map(
                  instance ->
                      pool.submit(
                          () -> {
                            ready.countDown();
                            if (!start.await(10, TimeUnit.SECONDS)) {
                              throw new IllegalStateException("Concurrent start timed out");
                            }
                            instance.updateAccountsBalances(DATE);
                            return null;
                          }))
              .toList();
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      for (var future : futures) {
        future.get(20, TimeUnit.SECONDS);
      }
    } finally {
      start.countDown();
      pool.shutdownNow();
    }
    assertThat(accounts.findAll())
        .allSatisfy(
            account -> assertThat(account.getAvailableBalanceMinorUnits()).isEqualTo(10002));
    assertThat(accruals.count()).isEqualTo(2);
    assertThat(
            metrics.get("account.interest.accruals").tag("result", "processed").counter().count())
        .isEqualTo(2);
    assertThat(metrics.get("account.interest.accruals").tag("result", "skipped").counter().count())
        .isEqualTo(2);
    assertThat(metrics.find("account.interest.accruals").tag("result", "failed").counter())
        .isNull();
  }

  @Test
  void rollbackRemovesHistoryAndRetryCreditsOnlyFailedAccount() {
    createAccount(10000);
    var jdbc = new JdbcTemplate(dataSource);
    jdbc.execute(
        """
        CREATE FUNCTION reject_interest_update() RETURNS trigger AS $$
        BEGIN
          IF NEW.id = '%s'::uuid THEN
            RAISE EXCEPTION 'Simulated failure after history insert';
          END IF;
          RETURN NEW;
        END;
        $$ LANGUAGE plpgsql
        """
            .formatted(accountId));
    jdbc.execute(
        "CREATE TRIGGER reject_interest BEFORE UPDATE ON accounts FOR EACH ROW EXECUTE FUNCTION reject_interest_update()");
    try {
      scheduler().updateAccountsBalances(DATE);
      assertThat(balance()).isEqualTo(10000);
      assertThat(accruals.existsByAccountIdAndAccrualDate(accountId, DATE)).isFalse();
      assertThat(accruals.count()).isEqualTo(1);
    } finally {
      jdbc.execute("DROP TRIGGER reject_interest ON accounts");
      jdbc.execute("DROP FUNCTION reject_interest_update()");
    }
    scheduler().updateAccountsBalances(DATE);
    assertThat(accounts.findAll())
        .allSatisfy(
            account -> assertThat(account.getAvailableBalanceMinorUnits()).isEqualTo(10002));
    assertThat(accruals.count()).isEqualTo(2);
    assertThat(metrics.get("account.interest.accruals").tag("result", "failed").counter().count())
        .isEqualTo(1);
    assertThat(
            metrics.get("account.interest.accruals").tag("result", "processed").counter().count())
        .isEqualTo(2);
    assertThat(metrics.get("account.interest.accruals").tag("result", "skipped").counter().count())
        .isEqualTo(1);
  }

  @Test
  void databaseRejectsDuplicateDateWithoutAbortingTransaction() {
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> {
              assertThat(
                      accruals.tryInsert(
                          UUID.randomUUID(), accountId, DATE, 0, new BigDecimal("7")))
                  .isEqualTo(1);
              assertThat(
                      accruals.tryInsert(
                          UUID.randomUUID(), accountId, DATE, 0, new BigDecimal("7")))
                  .isZero();
            });
    assertThat(accruals.count()).isEqualTo(1);
  }

  @Test
  void zeroInterestStillMarksDateAsProcessed() {
    var account = accounts.findById(accountId).orElseThrow();
    account.setAvailableBalanceMinorUnits(1L);
    accounts.saveAndFlush(account);
    assertThat(interest.accrueInterest(accountId, DATE)).isTrue();
    assertThat(interest.accrueInterest(accountId, DATE)).isFalse();
    assertThat(balance()).isEqualTo(1);
    assertThat(accruals.findAll())
        .singleElement()
        .satisfies(accrual -> assertThat(accrual.getAmountMinorUnits()).isZero());
  }
}
