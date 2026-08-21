package cardservice.service;

import cardservice.exception.CardNotFoundException;
import cardservice.repository.CardLimitHoldRepository;
import cardservice.repository.CardRepository;
import enums.account.ReservationStatus;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CardScheduler {
  private final CardRepository cardRepository;
  private final CardLimitHoldRepository cardLimitHoldRepository;

  public CardScheduler(
      CardLimitHoldRepository cardLimitHoldRepository, CardRepository cardRepository) {
    this.cardLimitHoldRepository = cardLimitHoldRepository;
    this.cardRepository = cardRepository;
  }

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
                  card.getSpendDailyLimitMinorUnits().subtract(limitsHold.getMinorUnits()));
              card.setSpendMonthlyLimitMinorUnits(
                  card.getSpendMonthlyLimitMinorUnits().subtract(limitsHold.getMinorUnits()));
              cardRepository.save(card);
              limitsHold.setStatus(ReservationStatus.RELEASED_BY_TIME);
              limitsHold.setReleasedAt(LocalDateTime.now());
            });

    cardLimitHoldRepository.saveAll(limitsHolds);
  }

  @Scheduled(cron = "0 0 0 * * *")
  @Transactional
  public void resetSpendDailyLimit() {
    cardRepository.resetSpendDailyLimitMinorUnits(BigDecimal.ZERO);
  }

  @Scheduled(cron = "0 0 0 1 * *")
  @Transactional
  public void resetSpendMonthlyLimit() {
    cardRepository.resetSpendMonthlyLimitMinorUnits(BigDecimal.ZERO);
  }
}
