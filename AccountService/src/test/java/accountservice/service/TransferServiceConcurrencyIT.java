package accountservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import accountservice.dto.ReserveFundsForTransactionCommand;
import accountservice.dto.ReserveFundsForTransactionResult;
import accountservice.entity.AccountEntity;
import accountservice.entity.CurrencyEntity;
import accountservice.mapper.result.AccountResultMapper;
import accountservice.repository.AccountHoldRepository;
import accountservice.repository.AccountRepository;
import accountservice.repository.CurrencyRepository;
import enums.account.AccountStatus;
import enums.account.AccountType;
import enums.account.ReservationStatus;
import enums.common.Currency;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
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
@DataJpaTest
@Import({TransferService.class, CurrencyService.class, AccountOutboxService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class TransferServiceConcurrencyIT {

  private static final UUID OWNER_USER_ID = UUID.randomUUID();
  private static final UUID OWNER_AUTH_USER_ID = UUID.randomUUID();

  private static final String SOURCE_ACCOUNT_NUMBER = "12345678";
  private static final String TARGET_ACCOUNT_NUMBER = "87654321";
  private static UUID SOURCE_ACCOUNT_ID = null;
  private static UUID TARGET_ACCOUNT_ID = null;

  @Container @ServiceConnection
  static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16-alpine");

  @Autowired private TransferService transferService;

  @MockitoBean private CurrencyService currencyService;

  @MockitoBean private AccountOutboxService accountOutboxService;

  @MockitoBean private AccountResultMapper resultMapper;

  @Autowired private AccountRepository accountRepository;

  @Autowired private CurrencyRepository currencyRepository;

  @Autowired private AccountHoldRepository accountHoldRepository;

  @BeforeEach
  void prepare() {
    cleanDB();
    prepareDB();
  }

  void cleanDB() {
    accountRepository.deleteAll();
    accountHoldRepository.deleteAll();
  }

  void prepareDB() {
    var currency = currencyRepository.findByName(Currency.USD);

    var sourceAccount = prepareAccountEntity(currency, SOURCE_ACCOUNT_NUMBER, AccountType.CHECKING);
    accountRepository.saveAndFlush(sourceAccount);
    SOURCE_ACCOUNT_ID = sourceAccount.getId();

    var targetAccount = prepareAccountEntity(currency, TARGET_ACCOUNT_NUMBER, AccountType.SAVINGS);
    accountRepository.saveAndFlush(targetAccount);
    TARGET_ACCOUNT_ID = targetAccount.getId();
  }

  void topUpAccount(Long topUpAmount, UUID accountId) {
    var account =
        accountRepository
            .findById(accountId)
            .orElseThrow(() -> new IllegalStateException("Account not found"));
    account.setAvailableBalanceMinorUnits(account.getAvailableBalanceMinorUnits() + topUpAmount);
    accountRepository.saveAndFlush(account);
  }

  AccountEntity prepareAccountEntity(
      CurrencyEntity currency, String accountNumber, AccountType type) {
    AccountEntity account = new AccountEntity();
    account.setOwnerUserId(OWNER_USER_ID);
    account.setOwnerAuthUserId(OWNER_AUTH_USER_ID);
    account.setAccountNumber(accountNumber);
    account.setAccountType(type);
    account.setAccountStatus(AccountStatus.ACTIVE);
    account.setAvailableBalanceMinorUnits(0L);
    account.setReservedBalanceMinorUnits(0L);
    account.setCurrency(currency);
    return account;
  }

  @Test
  void shouldReserveFundsForTransactionWithConcurrency() {
    topUpAccount(10000L, SOURCE_ACCOUNT_ID);

    int threads = 2;

    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch ready = new CountDownLatch(threads);
    List<Future<ReserveFundsForTransactionResult>> futures = new ArrayList<>();

    UUID firstTransactionId = UUID.randomUUID();
    UUID secondTransactionId = UUID.randomUUID();

    List<UUID> transactionIds = new ArrayList<>();
    transactionIds.add(firstTransactionId);
    transactionIds.add(secondTransactionId);

    try {
      for (int i = 0; i < threads; i++) {
        final int finalI = i;
        futures.add(
            pool.submit(
                () -> {
                  ready.countDown();
                  start.await();
                  ReserveFundsForTransactionCommand command =
                      new ReserveFundsForTransactionCommand(
                          SOURCE_ACCOUNT_ID,
                          TARGET_ACCOUNT_ID,
                          7000L,
                          transactionIds.get(finalI),
                          OWNER_AUTH_USER_ID,
                          Currency.USD);
                  return transferService.reserveFundsForTransactional(command);
                }));
      }

      assertThat(ready.await(5, TimeUnit.SECONDS)).as("Оба потока готовы к старту").isTrue();
      start.countDown();

      List<ReservationStatus> statuses = new ArrayList<>();

      for (Future<ReserveFundsForTransactionResult> f : futures) {
        var r = f.get(10, TimeUnit.SECONDS);
        statuses.add(r.status());
      }

      assertThat(statuses)
          .containsExactlyInAnyOrder(ReservationStatus.RESERVED, ReservationStatus.FAILED);

      var account = accountRepository.findById(SOURCE_ACCOUNT_ID).get();
      assertThat(account.getReservedBalanceMinorUnits()).isEqualTo(7000L);
      assertThat(account.getAvailableBalanceMinorUnits()).isEqualTo(10000L);

      assertThat(accountHoldRepository.count()).isEqualTo(1);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    } finally {
      pool.shutdownNow();
    }
  }
}
