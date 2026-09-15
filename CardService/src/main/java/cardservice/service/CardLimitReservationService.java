package cardservice.service;

import cardservice.dto.ReserveLimitsForTransactionCommand;
import cardservice.dto.ReserveLimitsForTransactionResult;
import cardservice.entity.CardEntity;
import cardservice.entity.CardLimitHoldEntity;
import cardservice.exception.*;
import cardservice.repository.AccountOwnershipProjectionRepository;
import cardservice.repository.CardLimitHoldRepository;
import cardservice.repository.CardRepository;
import enums.account.ReservationStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardLimitReservationService {
  private static final long HOLD_TTL_MINUTES = 5;
  private final CardRepository cardRepository;
  private final AccountOwnershipProjectionRepository accountOwnershipProjectionRepository;
  private final CardLimitHoldRepository cardLimitHoldRepository;

  @Transactional
  public ReserveLimitsForTransactionResult reserve(ReserveLimitsForTransactionCommand command) {
    if (cardLimitHoldRepository.existsByTransactionId(command.transactionId())) {
      throw new CardLimitHoldAlreadyExistsException(command.transactionId());
    }

    var isAmountNegative = command.minorUnits().compareTo(Long.valueOf(0)) < 0;

    if (isAmountNegative) {
      throw new InvalidTransactionAmountException(command.transactionId());
    }

    var card =
        cardRepository
            .findByIdForUpdate(command.sourceCardId())
            .orElseThrow(() -> new CardNotFoundException(command.sourceCardId()));

    if (!isAccountOwnedBy(card.getAccountId(), command.sourceAuthUserId())) {
      throw new CardNotFoundException(command.sourceCardId());
    }

    if (card.getCurrency() != command.currency()) {
      throw new CardCurrencyMismatchException(
          command.transactionId(), card.getCurrency(), command.currency());
    }

    validateLimitsForTransaction(command, card);

    createCardLimitHold(command, card);

    reserveLimitOnCard(command, card);

    return new ReserveLimitsForTransactionResult(
        ReservationStatus.RESERVED, "Limits reserved for transaction " + command.transactionId());
  }

  private void createCardLimitHold(ReserveLimitsForTransactionCommand command, CardEntity card) {
    var carLimitHold = new CardLimitHoldEntity();
    carLimitHold.setCardId(card.getId());
    carLimitHold.setMinorUnits(command.minorUnits());
    carLimitHold.setTransactionId(command.transactionId());
    carLimitHold.setStatus(ReservationStatus.RESERVED);
    carLimitHold.setExpiresAt(LocalDateTime.now().plusMinutes(HOLD_TTL_MINUTES));
    cardLimitHoldRepository.save(carLimitHold);
  }

  private void reserveLimitOnCard(ReserveLimitsForTransactionCommand command, CardEntity card) {
    card.setSpendDailyLimitMinorUnits(card.getSpendDailyLimitMinorUnits() + command.minorUnits());
    card.setSpendMonthlyLimitMinorUnits(
        card.getSpendMonthlyLimitMinorUnits() + command.minorUnits());
    cardRepository.save(card);
  }

  private boolean isAccountOwnedBy(UUID accountId, UUID authUserId) {
    return authUserId != null
        && accountOwnershipProjectionRepository
            .findById(accountId)
            .map(projection -> projection.getOwnerAuthUserId().equals(authUserId))
            .orElse(false);
  }

  private void validateLimitsForTransaction(
      ReserveLimitsForTransactionCommand command, CardEntity card) {
    Long availableDailyLimits =
        card.getDailyLimitMinorUnits() - card.getSpendDailyLimitMinorUnits();
    Long availableMonthlyLimits =
        card.getMonthlyLimitMinorUnits() - card.getSpendMonthlyLimitMinorUnits();

    if (availableDailyLimits.compareTo(command.minorUnits()) < 0) {
      throw new InsufficientDailyCardLimitException(command.transactionId());
    }

    if (availableMonthlyLimits.compareTo(command.minorUnits()) < 0) {
      throw new InsufficientMonthlyCardLimitException(command.transactionId());
    }
  }
}
