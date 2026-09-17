package accountservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import accountservice.entity.AccountEntity;
import accountservice.entity.CurrencyEntity;
import accountservice.repository.AccountInterestAccrualRepository;
import accountservice.repository.AccountRepository;
import enums.account.AccountType;
import enums.common.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AccountInterestServiceTest {
  private final AccountRepository accounts = mock(AccountRepository.class);
  private final AccountInterestAccrualRepository accruals =
      mock(AccountInterestAccrualRepository.class);
  private final AccountOverviewCacheInvalidationService cacheInvalidationService =
      mock(AccountOverviewCacheInvalidationService.class);
  private final AccountInterestService service =
      new AccountInterestService(accounts, accruals, cacheInvalidationService);
  private final UUID accountId = UUID.randomUUID();
  private final LocalDate date = LocalDate.of(2026, 9, 14);

  private AccountEntity account(Currency currency, long balance) {
    var entity = new AccountEntity();
    var currencyEntity = new CurrencyEntity();
    currencyEntity.setName(currency);
    entity.setCurrency(currencyEntity);
    entity.setAccountType(AccountType.SAVINGS);
    entity.setAvailableBalanceMinorUnits(balance);
    when(accounts.findByIdForUpdate(accountId)).thenReturn(Optional.of(entity));
    return entity;
  }

  @ParameterizedTest
  @EnumSource(Currency.class)
  void preservesCurrencyRoundingAndStoresRoundedDelta(Currency currency) {
    when(accruals.tryInsert(any(), eq(accountId), eq(date), anyLong(), eq(new BigDecimal("7"))))
        .thenReturn(1);
    for (long[] sample :
        new long[][] {
          {0, 0},
          {2607, 0},
          {2608, 1},
          {10000, 2},
          {250_000_000_000L, 47_945_206L},
          {750_000_000_000L, 143_835_616L}
        }) {
      var account = account(currency, sample[0]);
      assertThat(service.accrueInterest(accountId, date)).isTrue();
      assertThat(account.getAvailableBalanceMinorUnits()).isEqualTo(sample[0] + sample[1]);
    }
    verify(accruals).tryInsert(any(), eq(accountId), eq(date), eq(2L), eq(new BigDecimal("7")));
  }

  @Test
  void uniqueConflictDoesNotChangeBalance() {
    var account = account(Currency.USD, 10000);
    when(accruals.tryInsert(any(), any(), any(), anyLong(), any())).thenReturn(0);
    assertThat(service.accrueInterest(accountId, date)).isFalse();
    assertThat(account.getAvailableBalanceMinorUnits()).isEqualTo(10000);
    verify(accounts, never()).saveAndFlush(any());
  }

  @Test
  void existingAccrualSkipsEvenIfBalanceNowOverflowsInterestCalculation() {
    account(Currency.USD, Long.MAX_VALUE);
    when(accruals.existsByAccountIdAndAccrualDate(accountId, date)).thenReturn(true);
    assertThat(service.accrueInterest(accountId, date)).isFalse();
    verify(accruals, never()).tryInsert(any(), any(), any(), anyLong(), any());
  }

  @Test
  void checkingAccountDoesNotAccrue() {
    account(Currency.USD, 10000).setAccountType(AccountType.CHECKING);
    assertThat(service.accrueInterest(accountId, date)).isFalse();
    verifyNoInteractions(accruals);
  }
}
