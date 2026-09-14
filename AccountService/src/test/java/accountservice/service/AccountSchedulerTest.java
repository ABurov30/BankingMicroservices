package accountservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import accountservice.repository.AccountHoldRepository;
import accountservice.repository.AccountRepository;
import enums.account.AccountType;
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
  private final AccountInterestService interest = mock(AccountInterestService.class);
  private final SimpleMeterRegistry metrics = new SimpleMeterRegistry();

  private AccountScheduler scheduler(Clock clock) {
    return new AccountScheduler(
        accounts, mock(AccountHoldRepository.class), interest, metrics, clock);
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
}
