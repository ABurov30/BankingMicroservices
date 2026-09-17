package accountservice.service;

import accountservice.exception.AccountsNotFoundException;
import accountservice.repository.AccountHoldRepository;
import accountservice.repository.AccountRepository;
import enums.account.AccountType;
import enums.account.ReservationStatus;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import kafkacontracts.account.AccountEventType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountScheduler {
  private final AccountRepository accountRepository;
  private final AccountHoldRepository accountHoldRepository;
  private final AccountInterestService interestService;
  private final MeterRegistry meterRegistry;
  private final AccountOutboxService accountOutboxService;
  private final AccountOverviewCacheInvalidationService cacheInvalidationService;
  private final Clock accountClock;
  private static final Logger log = LoggerFactory.getLogger(AccountScheduler.class);
  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Paris");

  @Scheduled(cron = "0 0 0 * * *", zone = "Europe/Paris")
  public void updateAccountsBalances() {
    updateAccountsBalances(LocalDate.now(accountClock.withZone(BUSINESS_ZONE)));
  }

  public void updateAccountsBalances(LocalDate businessDate) {
    Objects.requireNonNull(businessDate, "businessDate");
    log.info("Starting savings interest accrual: businessDate={}", businessDate);
    int processed = 0;
    int skipped = 0;
    int failed = 0;
    for (var accountId : accountRepository.findIdsByAccountType(AccountType.SAVINGS)) {
      try {
        boolean accrued = interestService.accrueInterest(accountId, businessDate);
        meterRegistry
            .counter("account.interest.accruals", "result", accrued ? "processed" : "skipped")
            .increment();
        if (accrued) {
          processed++;
        } else {
          skipped++;
        }
      } catch (RuntimeException exception) {
        failed++;
        meterRegistry.counter("account.interest.accruals", "result", "failed").increment();
        log.error(
            "Savings interest accrual failed: businessDate={}, accountId={},"
                + " idempotencyKey=interest:{}:{}",
            businessDate,
            accountId,
            accountId,
            businessDate,
            exception);
      }
    }
    log.info(
        "Finished savings interest accrual: businessDate={}, processed={}, skipped={}, failed={}",
        businessDate,
        processed,
        skipped,
        failed);
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

              account.setReservedBalanceMinorUnits(
                  account.getReservedBalanceMinorUnits() - accountHold.getMinorUnits());

              accountRepository.save(account);
              cacheInvalidationService.invalidate(account);
              accountHold.setStatus(ReservationStatus.RELEASED_BY_TIME);
              accountHold.setReleasedAt(LocalDateTime.now());
              accountOutboxService.saveAccountOutboxEvent(
                  accountHold.getTransactionId(),
                  AccountEventType.ACCOUNT_HOLD_RELEASED_BY_TIME,
                  Map.of("transactionId", accountHold.getTransactionId()));
            });

    accountHoldRepository.saveAll(accountHolds);
  }
}
