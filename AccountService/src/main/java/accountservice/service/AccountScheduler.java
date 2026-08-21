package accountservice.service;

import accountservice.exception.AccountsNotFoundException;
import accountservice.repository.AccountHoldRepository;
import accountservice.repository.AccountRepository;
import enums.account.AccountType;
import enums.account.ReservationStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountScheduler {
  private final AccountRepository accountRepository;
  private final AccountHoldRepository accountHoldRepository;
  private static final BigDecimal DEBIT_RATE = new BigDecimal("7");

  public AccountScheduler(
      AccountRepository accountRepository, AccountHoldRepository accountHoldRepository) {
    this.accountRepository = accountRepository;
    this.accountHoldRepository = accountHoldRepository;
  }

  @Scheduled(cron = "0 0 0 * * *", zone = "Europe/Paris")
  @Transactional
  public void updateAccountsBalances() {
    var accounts =
        accountRepository
            .findAllByTypeForUpdate(AccountType.SAVINGS)
            .orElseThrow(() -> new AccountsNotFoundException(AccountType.SAVINGS));
    BigDecimal dailyMultiplier =
        BigDecimal.ONE.add(DEBIT_RATE.divide(BigDecimal.valueOf(36_500), 12, RoundingMode.HALF_UP));
    accounts.stream()
        .forEach(
            account -> {
              BigDecimal newBalance =
                  account
                      .getAvailableBalance()
                      .multiply(dailyMultiplier)
                      .setScale(2, RoundingMode.HALF_UP);

              account.setAvailableBalance(newBalance);
            });

    accountRepository.saveAll(accounts);
  }

  @Scheduled(fixedDelay = 5000)
  @Transactional
  public void releaseFundsForTransactionByTime() {
    var accountHolds =
        accountHoldRepository
            .findForUpdateTop50ByReservationStatusAndExpiresAtLessThanEqualOrderByCreatedAtAsc(
                ReservationStatus.RESERVED, LocalDateTime.now(), PageRequest.of(0, 50));

    accountHolds.stream()
        .forEach(
            (accountHold) -> {
              var account =
                  accountRepository
                      .findByIdForUpdate(accountHold.getAccountId())
                      .orElseThrow(() -> new AccountsNotFoundException(accountHold.getAccountId()));
              var accountCurrency = account.getCurrency().getName();

              account.setReservedBalance(
                  account
                      .getReservedBalance()
                      .subtract(
                          accountHold
                              .getMinorUnits()
                              .movePointLeft(accountCurrency.getMinorUnit())));

              accountRepository.save(account);
              accountHold.setStatus(ReservationStatus.RELEASED_BY_TIME);
              accountHold.setReleasedAt(LocalDateTime.now());
            });

    accountHoldRepository.saveAll(accountHolds);
  }
}
