package accountservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import accountservice.entity.AccountEntity;
import accountservice.entity.AccountHoldEntity;
import accountservice.repository.AccountHoldRepository;
import accountservice.repository.AccountRepository;
import enums.account.AccountType;
import enums.account.ReservationStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AccountSchedulerTest {
  private final AccountRepository accounts = mock(AccountRepository.class);
  private final AccountOutboxService outbox = mock(AccountOutboxService.class);
  private final AccountHoldRepository holds = mock(AccountHoldRepository.class);
  private final AccountInterestService interest = mock(AccountInterestService.class);
  private final AccountOverviewCacheInvalidationService cacheInvalidationService =
      mock(AccountOverviewCacheInvalidationService.class);
  private final SimpleMeterRegistry metrics = new SimpleMeterRegistry();

  private AccountScheduler scheduler(Clock clock) {
    return new AccountScheduler(
        accounts, holds, interest, metrics, outbox, cacheInvalidationService, clock);
  }

  @ParameterizedTest
  @CsvSource({"2026-01-01T23:30:00Z,2026-01-02", "2026-07-01T22:30:00Z,2026-07-02"})
  void usesParisBusinessDateForEveryAccount(String instant, LocalDate expectedDate) {
    var first = UUID.randomUUID();
    var second = UUID.randomUUID();
    when(accounts.findIdsByAccountType(AccountType.SAVINGS)).thenReturn(List.of(first, second));
    scheduler(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)).updateAccountsBalances();
    verify(interest).accrueInterest(first, expectedDate);
    verify(interest).accrueInterest(second, expectedDate);
  }

  @Test
  void emptySelectionIsSuccessful() {
    when(accounts.findIdsByAccountType(AccountType.SAVINGS)).thenReturn(List.of());
    scheduler(Clock.systemUTC()).updateAccountsBalances();
    verifyNoInteractions(interest);
  }

  @Test
  void countsCommittedResultsAndContinuesAfterFailure() {
    LocalDate date = LocalDate.of(2026, 9, 14);
    var failed = UUID.randomUUID();
    var processed = UUID.randomUUID();
    var skipped = UUID.randomUUID();
    when(accounts.findIdsByAccountType(AccountType.SAVINGS))
        .thenReturn(List.of(failed, processed, skipped));
    when(interest.accrueInterest(failed, date))
        .thenThrow(new IllegalStateException("DB unavailable"));
    when(interest.accrueInterest(processed, date)).thenReturn(true);
    when(interest.accrueInterest(skipped, date)).thenReturn(false);
    scheduler(Clock.systemUTC()).updateAccountsBalances(date);
    for (String result : List.of("processed", "skipped", "failed")) {
      assertThat(metrics.get("account.interest.accruals").tag("result", result).counter().count())
          .isEqualTo(1);
    }
  }

  @Test
  void releasesExpiredHoldAndWritesOutboxEvent() {
    var accountId = UUID.randomUUID();
    var transactionId = UUID.randomUUID();
    var account = new AccountEntity();
    account.setReservedBalanceMinorUnits(100L);
    var hold = new AccountHoldEntity();
    hold.setAccountId(accountId);
    hold.setTransactionId(transactionId);
    hold.setMinorUnits(40L);
    hold.setStatus(ReservationStatus.RESERVED);
    when(holds.findForUpdateTop50ByReservationStatusAndExpiresAtLessThanEqualOrderByCreatedAtAsc(
            eq(ReservationStatus.RESERVED), any(), any()))
        .thenReturn(List.of(hold));
    when(accounts.findByIdForUpdate(accountId)).thenReturn(java.util.Optional.of(account));

    scheduler(Clock.systemUTC()).releaseFundsForTransactionByTime();

    assertThat(account.getReservedBalanceMinorUnits()).isEqualTo(60L);
    assertThat(hold.getStatus()).isEqualTo(ReservationStatus.RELEASED_BY_TIME);
    verify(outbox)
        .saveAccountOutboxEvent(
            eq(transactionId),
            eq(kafkacontracts.account.AccountEventType.ACCOUNT_HOLD_RELEASED_BY_TIME),
            any());
  }
}
