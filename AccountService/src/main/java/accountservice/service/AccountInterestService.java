package accountservice.service;

import accountservice.exception.AccountsNotFoundException;
import accountservice.repository.AccountInterestAccrualRepository;
import accountservice.repository.AccountRepository;
import enums.account.AccountType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import moneyunitsconverter.MoneyUnitsConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountInterestService {
  private static final BigDecimal ANNUAL_RATE_PERCENT = new BigDecimal("7");
  private static final BigDecimal DAILY_MULTIPLIER =
      BigDecimal.ONE.add(
          ANNUAL_RATE_PERCENT.divide(BigDecimal.valueOf(36_500), 12, RoundingMode.HALF_UP));

  private final AccountRepository accountRepository;
  private final AccountInterestAccrualRepository accrualRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean accrueInterest(UUID accountId, LocalDate businessDate) {
    Objects.requireNonNull(businessDate, "businessDate");
    var account =
        accountRepository
            .findByIdForUpdate(accountId)
            .orElseThrow(() -> new AccountsNotFoundException(accountId));
    if (account.getAccountType() != AccountType.SAVINGS
        || accrualRepository.existsByAccountIdAndAccrualDate(accountId, businessDate)) {
      return false;
    }

    var currency = account.getCurrency().getName();
    long previousBalance = account.getAvailableBalanceMinorUnits();
    long newBalance =
        MoneyUnitsConverter.toMinor(
            MoneyUnitsConverter.toMajor(previousBalance, currency).multiply(DAILY_MULTIPLIER),
            currency);
    long amount = Math.subtractExact(newBalance, previousBalance);
    if (accrualRepository.tryInsert(
            UUID.randomUUID(), accountId, businessDate, amount, ANNUAL_RATE_PERCENT)
        == 0) {
      return false;
    }
    account.setAvailableBalanceMinorUnits(newBalance);
    accountRepository.saveAndFlush(account);
    return true;
  }
}
