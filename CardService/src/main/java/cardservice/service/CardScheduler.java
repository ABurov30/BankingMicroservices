package cardservice.service;

import cardservice.exception.CardNotFoundException;
import cardservice.repository.CardLimitHoldRepository;
import cardservice.repository.CardRepository;
import enums.account.ReservationStatus;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Map;
import kafkacontracts.card.CardEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardScheduler {
  private final CardRepository cardRepository;
  private final CardLimitHoldRepository cardLimitHoldRepository;
  private final CardOutboxService cardOutboxService;
  private final AccountOverviewCacheInvalidationService cacheInvalidationService;

  @Scheduled(fixedDelay = 5000)
  @Transactional
  public void releaseLimitsForTransactionByTime() {
    var limitsHolds =
        cardLimitHoldRepository
            .findForUpdateTop50ByReservationStatusAndExpiresAtLessThanEqualOrderByCreatedAtAsc(
                ReservationStatus.RESERVED, LocalDateTime.now(), PageRequest.of(0, 50));

    limitsHolds.stream()
        .forEach(
            (limitsHold) -> {
              var card =
                  cardRepository
                      .findByIdForUpdate(limitsHold.getCardId())
                      .orElseThrow(() -> new CardNotFoundException(limitsHold.getCardId()));

              card.setSpendDailyLimitMinorUnits(
                  card.getSpendDailyLimitMinorUnits() - limitsHold.getMinorUnits());
              card.setSpendMonthlyLimitMinorUnits(
                  card.getSpendMonthlyLimitMinorUnits() - limitsHold.getMinorUnits());
              cardRepository.save(card);
              cacheInvalidationService.invalidate(card.getId(), card.getAccountId());
              limitsHold.setStatus(ReservationStatus.RELEASED_BY_TIME);
              limitsHold.setReleasedAt(LocalDateTime.now());
              cardOutboxService.saveCardOutboxEvent(
                  limitsHold.getTransactionId(),
                  CardEventType.CARD_LIMIT_HOLD_RELEASED_BY_TIME,
                  Map.of("transactionId", limitsHold.getTransactionId()));
            });

    cardLimitHoldRepository.saveAll(limitsHolds);
  }

  @Scheduled(cron = "0 0 0 * * *")
  @Transactional
  public void resetSpendDailyLimit() {
    if (cardRepository.resetSpendDailyLimitMinorUnits(Long.valueOf(0)) > 0) {
      cacheInvalidationService.invalidateAll();
    }
  }

  @Scheduled(cron = "0 0 0 1 * *")
  @Transactional
  public void resetSpendMonthlyLimit() {
    if (cardRepository.resetSpendMonthlyLimitMinorUnits(Long.valueOf(0)) > 0) {
      cacheInvalidationService.invalidateAll();
    }
  }
}
