package cardservice.service;

import cardservice.dto.*;
import cardservice.entity.AccountOwnershipProjectionEntity;
import cardservice.entity.CardEntity;
import cardservice.entity.CardLimitHoldEntity;
import cardservice.entity.CardOutboxEventEntity;
import cardservice.exception.CardBlockedException;
import cardservice.exception.CardCurrencyMismatchException;
import cardservice.exception.CardExpiredException;
import cardservice.exception.CardGenerationFailedException;
import cardservice.exception.CardLimitHoldAlreadyExistsException;
import cardservice.exception.CardNotFoundException;
import cardservice.exception.CardsNotFoundException;
import cardservice.exception.InsufficientDailyCardLimitException;
import cardservice.exception.InsufficientMonthlyCardLimitException;
import cardservice.exception.InvalidCardLimitException;
import cardservice.exception.InvalidTransactionAmountException;
import cardservice.mapper.result.CardResultMapper;
import cardservice.repository.AccountOwnershipProjectionRepository;
import cardservice.repository.CardLimitHoldRepository;
import cardservice.repository.CardOutboxEventRepository;
import cardservice.repository.CardRepository;
import enums.account.ReservationStatus;
import enums.card.CardStatus;
import enums.common.Currency;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import kafkacontracts.card.CardEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardService {
  private final CardRepository cardRepository;
  private final AccountOwnershipProjectionRepository accountOwnershipProjectionRepository;
  private final CardOutboxEventRepository cardOutboxEventRepository;
  private final CardLimitHoldRepository cardLimitHoldRepository;
  private final CardResultMapper resultMapper;
  private static final int CARD_EXPIRATION_YEARS = 5;
  private static final String CARD_BIN = "400000";
  private static final int PAN_LENGTH = 16;
  private static final int ATTEMPTS_TO_GENERATE_PAN = 10;
  private static final long HOLD_TTL_MINUTES = 5;
  private static final Logger log = LoggerFactory.getLogger(CardService.class);

  public CardService(
      CardRepository cardRepository,
      AccountOwnershipProjectionRepository accountOwnershipProjectionRepository,
      CardOutboxEventRepository cardOutboxEventRepository,
      CardResultMapper resultMapper,
      CardLimitHoldRepository cardLimitHoldRepository) {
    this.cardRepository = cardRepository;
    this.accountOwnershipProjectionRepository = accountOwnershipProjectionRepository;
    this.cardOutboxEventRepository = cardOutboxEventRepository;
    this.resultMapper = resultMapper;
    this.cardLimitHoldRepository = cardLimitHoldRepository;
  }

  private String generateUniquePan() {

    for (int i = 0; i < ATTEMPTS_TO_GENERATE_PAN; i++) {
      String pan = generatePan();
      if (!cardRepository.existsByPan(pan)) {
        return pan;
      }
    }

    throw new CardGenerationFailedException("Failed to generate unique pan");
  }

  private String generatePan() {
    StringBuilder panWithoutCheckDigit = new StringBuilder(CARD_BIN);

    while (panWithoutCheckDigit.length() < PAN_LENGTH - 1) {
      panWithoutCheckDigit.append(ThreadLocalRandom.current().nextInt(10));
    }

    int checkDigit = calculateLuanCheckDigit(panWithoutCheckDigit.toString());

    return panWithoutCheckDigit.append(checkDigit).toString();
  }

  private int calculateLuanCheckDigit(String number) {
    int sum = 0;
    boolean doubleDigit = true;

    for (int i = number.length() - 1; i >= 0; i--) {
      int digit = Character.getNumericValue(number.charAt(i));

      if (doubleDigit) {
        digit *= 2;

        if (digit > 9) {
          digit -= 9;
        }
      }

      sum += digit;
      doubleDigit = !doubleDigit;
    }

    return (10 - (sum % 10)) % 10;
  }

  @Transactional
  public CreateCardResult createCard(CreatedCardCommand createdCardCommand) {
    return createCard(createdCardCommand, null);
  }

  @Transactional
  public CreateCardResult createCard(
      CreatedCardCommand createdCardCommand, Currency accountCurrencyFromEvent) {
    if (createdCardCommand.role() != null
        && !canAccessAccount(
            createdCardCommand.accountId(),
            createdCardCommand.authUserId(),
            createdCardCommand.role())) {
      throw new CardsNotFoundException(createdCardCommand.accountId());
    }

    Currency accountCurrency =
        accountCurrencyFromEvent != null
            ? accountCurrencyFromEvent
            : getAccountCurrency(createdCardCommand.accountId());

    CardEntity cardEntity = new CardEntity();
    cardEntity.setAccountId(createdCardCommand.accountId());
    cardEntity.setPan(generateUniquePan());
    cardEntity.setCardStatus(CardStatus.ACTIVE);
    cardEntity.setCurrency(createdCardCommand.currency());
    cardEntity.setDailyLimitMinorUnits(Long.valueOf(0));
    cardEntity.setMonthlyLimitMinorUnits(Long.valueOf(0));
    cardEntity.setExpiresAt(LocalDateTime.now().plusYears(CARD_EXPIRATION_YEARS));
    CardEntity savedCard = cardRepository.save(cardEntity);

    if (createdCardCommand.authUserId() != null && createdCardCommand.accountNumber() != null) {
      AccountOwnershipProjectionEntity accountOwnershipProjectionEntity =
          new AccountOwnershipProjectionEntity();
      accountOwnershipProjectionEntity.setOwnerAuthUserId(createdCardCommand.authUserId());
      accountOwnershipProjectionEntity.setAccountId(createdCardCommand.accountId());
      accountOwnershipProjectionEntity.setAccountNumber(createdCardCommand.accountNumber());
      accountOwnershipProjectionEntity.setCurrency(accountCurrency);
      accountOwnershipProjectionRepository.save(accountOwnershipProjectionEntity);
    }

    if (createdCardCommand.accountNumber() != null) {
      createCardOutboxEvent(
          CardEventType.CARD_CREATED,
          savedCard,
          createdCardCommand.authUserId(),
          createdCardCommand.accountNumber());
    }
    return resultMapper.toCreateCardResult(savedCard, accountCurrency);
  }

  private void changeCardStatus(CardEntity cardEntity, CardStatus cardStatus) {
    cardEntity.setCardStatus(cardStatus);
  }

  private void changeCardDailyLimit(
      CardEntity cardEntity, Long newDailyLimit, Long newMonthlyLimit) {
    Long monthlyLimit =
        newMonthlyLimit != null ? newMonthlyLimit : cardEntity.getMonthlyLimitMinorUnits();

    if (newDailyLimit.compareTo(monthlyLimit) > 0) {
      throw new InvalidCardLimitException("Daily limit should be less or equals monthly limit");
    }

    cardEntity.setDailyLimitMinorUnits(newDailyLimit);
  }

  private void changeCardMonthlyLimit(CardEntity cardEntity, Long newMonthlyLimit) {
    cardEntity.setMonthlyLimitMinorUnits(newMonthlyLimit);
  }

  @Transactional
  public void freezeCards(FreezeCardsCommand freezeCardsCommand) {
    var cards = cardRepository.findAllByAccountId(freezeCardsCommand.accountId());

    if (cards.isEmpty() || cards.get().isEmpty()) {
      log.warn(
          "Skipping cards freeze: accountId={} cards not found", freezeCardsCommand.accountId());
      return;
    }

    cards
        .get()
        .forEach(
            card -> {
              if (card.getCardStatus() == CardStatus.BLOCKED
                  || card.getCardStatus() == CardStatus.FROZEN) {
                log.info(
                    "Skipping card freeze: cardId={}, status={}",
                    card.getId(),
                    card.getCardStatus());
                return;
              }

              changeCardStatus(card, CardStatus.FROZEN);
              createCardOutboxEvent(
                  CardEventType.CARD_FROZEN,
                  card,
                  freezeCardsCommand.authUserId(),
                  freezeCardsCommand.accountNumber());
            });

    cardRepository.saveAll(cards.get());
  }

  @Transactional
  public void unfreezeCards(UnfreezeCardsCommand unfreezeCardsCommand) {
    var cards = cardRepository.findAllByAccountId(unfreezeCardsCommand.accountId());

    if (cards.isEmpty() || cards.get().isEmpty()) {
      log.warn(
          "Skipping cards unfreeze: accountId={} cards not found",
          unfreezeCardsCommand.accountId());
      return;
    }

    cards
        .get()
        .forEach(
            card -> {
              if (card.getCardStatus() != CardStatus.FROZEN) {
                log.info(
                    "Skipping card unfreeze: cardId={}, status={}",
                    card.getId(),
                    card.getCardStatus());
                return;
              }

              changeCardStatus(card, CardStatus.ACTIVE);
              createCardOutboxEvent(
                  CardEventType.CARD_UNFROZEN,
                  card,
                  unfreezeCardsCommand.authUserId(),
                  unfreezeCardsCommand.accountNumber());
            });

    cardRepository.saveAll(cards.get());
  }

  @Transactional
  public UpdateCardResult updateCard(UpdateCardCommand updateCardCommand) {
    CardEntity cardEntity =
        cardRepository
            .findById(updateCardCommand.cardId())
            .orElseThrow(() -> new CardNotFoundException(updateCardCommand.cardId()));

    if (!canAccessAccount(
        cardEntity.getAccountId(), updateCardCommand.authUserId(), updateCardCommand.role())) {
      throw new CardNotFoundException(updateCardCommand.cardId());
    }

    if (cardEntity.getCardStatus() == CardStatus.EXPIRED) {
      throw new CardExpiredException(cardEntity.getId());
    }

    if (cardEntity.getCardStatus() == CardStatus.BLOCKED) {
      throw new CardBlockedException(cardEntity.getId());
    }

    if (updateCardCommand.status() != null) {
      this.changeCardStatus(cardEntity, updateCardCommand.status());
    }

    if (updateCardCommand.dailyLimitMinorUnits() != null) {
      this.changeCardDailyLimit(
          cardEntity,
          updateCardCommand.dailyLimitMinorUnits(),
          updateCardCommand.monthlyLimitMinorUnits());
    }

    if (updateCardCommand.monthlyLimitMinorUnits() != null) {
      this.changeCardMonthlyLimit(cardEntity, updateCardCommand.monthlyLimitMinorUnits());
    }

    CardEntity savedCard = cardRepository.save(cardEntity);

    return resultMapper.toUpdateCardResult(savedCard, getAccountCurrency(savedCard.getAccountId()));
  }

  public List<GetCardResult> getCardsByAccountId(GetCardsByAccountIdCommand command) {
    List<CardEntity> cardEntityList = cardRepository.findByAccountId(command.accountId());
    if (cardEntityList.isEmpty()) {
      return List.of();
    }

    Currency accountCurrency = getAccountCurrency(command.accountId());
    return cardEntityList.stream()
        .map(card -> resultMapper.toGetCardResult(card, accountCurrency))
        .toList();
  }

  private Currency getAccountCurrency(UUID accountId) {
    return accountOwnershipProjectionRepository
        .findById(accountId)
        .map(AccountOwnershipProjectionEntity::getCurrency)
        .orElseThrow(() -> new CardsNotFoundException(accountId));
  }

  private boolean canAccessAccount(UUID accountId, UUID authUserId, String role) {
    if (authUserId == null) {
      return true;
    }

    if (isPrivileged(role)) {
      return true;
    }

    return accountOwnershipProjectionRepository
        .findById(accountId)
        .map(projection -> projection.getOwnerAuthUserId().equals(authUserId))
        .orElse(false);
  }

  private boolean isPrivileged(String role) {
    return "ADMIN".equals(role) || "MANAGER".equals(role);
  }

  private void createCardOutboxEvent(
      CardEventType eventType, CardEntity card, UUID authUserId, String accountNumber) {
    if (authUserId == null || accountNumber == null) {
      return;
    }

    CardOutboxEventEntity outboxEvent = new CardOutboxEventEntity();
    outboxEvent.setAggregateType("CARD");
    outboxEvent.setAggregateId(card.getId());
    outboxEvent.setEventType(eventType.name());
    outboxEvent.setTopic(eventType.getTopic());
    outboxEvent.setEventKey(card.getId() + ":" + eventType.name());
    outboxEvent.setSchemaVersion(eventType.getVersion());
    outboxEvent.setPayload(
        Map.of(
            "authUserId", authUserId,
            "accountId", card.getAccountId(),
            "accountNumber", accountNumber,
            "cardId", card.getId(),
            "cardNumber", card.getPan()));

    cardOutboxEventRepository.save(outboxEvent);
  }

  public ReserveLimitsForTransactionResult reserveLimitsForTransaction(
      ReserveLimitsForTransactionCommand command) {
    try {
      if (cardLimitHoldRepository.existsByTransactionId(command.transactionId())) {
        throw new CardLimitHoldAlreadyExistsException(command.transactionId());
      }

      var isAmountNegative = command.minorUnits().compareTo(Long.valueOf(0)) < 0;

      if (isAmountNegative) {
        throw new InvalidTransactionAmountException(command.transactionId());
      }

      var card =
          cardRepository
              .findById(command.sourceCardId())
              .orElseThrow(() -> new CardNotFoundException(command.sourceCardId()));

      if (card.getCurrency() != command.currency()) {
        throw new CardCurrencyMismatchException(
            command.transactionId(), card.getCurrency(), command.currency());
      }

      validateLimitsForTransaction(command, card);

      createCardLimitHold(command, card);

      reserveLimitOnCard(command, card);

      return new ReserveLimitsForTransactionResult(
          ReservationStatus.RESERVED, "Limits reserved for transaction " + command.transactionId());
    } catch (Exception e) {
      log.error("Failed to reserve limits: transactionId={}", command.transactionId(), e);
      return new ReserveLimitsForTransactionResult(ReservationStatus.FAILED, e.getMessage());
    }
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

  @Transactional
  public void compensateLimitsForTransaction(CompensateLimitsForTransactionCommand command) {
    var cardLimitHold =
        cardLimitHoldRepository.findByTransactionIdForUpdate(command.transactionId());

    if (cardLimitHold.isEmpty()) {
      log.warn("Card limit hold not found: transactionId={}", command.transactionId());
      return;
    }

    var limitHold = cardLimitHold.get();

    if (limitHold.getStatus() != ReservationStatus.RESERVED) {
      log.info(
          "Skipping card limit compensation: transactionId={}, status={}",
          command.transactionId(),
          limitHold.getStatus());
      return;
    }

    var card =
        cardRepository
            .findByIdForUpdate(limitHold.getCardId())
            .orElseThrow(() -> new CardNotFoundException(limitHold.getCardId()));
    var releasedAt = LocalDateTime.now();

    releaseReservedLimits(card, limitHold, releasedAt);
    limitHold.setStatus(ReservationStatus.COMPENSATED);
    limitHold.setReleasedAt(releasedAt);

    cardRepository.save(card);
    cardLimitHoldRepository.save(limitHold);
  }

  @Transactional
  public void markLimitReservationAsReleased(MarkLimitReservationAsReleasedCommand command) {
    var cardLimitHold =
        cardLimitHoldRepository.findByTransactionIdForUpdate(command.transactionId());

    if (cardLimitHold.isEmpty()) {
      log.warn("Card limit hold not found: transactionId={}", command.transactionId());
      return;
    }

    var limitHold = cardLimitHold.get();

    if (limitHold.getStatus() != ReservationStatus.RESERVED) {
      log.info(
          "Skipping card limit release: transactionId={}, status={}",
          command.transactionId(),
          limitHold.getStatus());
      return;
    }

    limitHold.setStatus(ReservationStatus.RELEASED);
    limitHold.setReleasedAt(LocalDateTime.now());

    cardLimitHoldRepository.save(limitHold);
  }

  private void releaseReservedLimits(
      CardEntity card, CardLimitHoldEntity limitHold, LocalDateTime releasedAt) {
    if (isSameDay(limitHold.getCreatedAt(), releasedAt)) {
      card.setSpendDailyLimitMinorUnits(
          card.getSpendDailyLimitMinorUnits() - limitHold.getMinorUnits());
    }

    if (isSameMonth(limitHold.getCreatedAt(), releasedAt)) {
      card.setSpendMonthlyLimitMinorUnits(
          card.getSpendMonthlyLimitMinorUnits() - limitHold.getMinorUnits());
    }
  }

  private boolean isSameDay(LocalDateTime createdAt, LocalDateTime releasedAt) {
    return createdAt == null || createdAt.toLocalDate().equals(releasedAt.toLocalDate());
  }

  private boolean isSameMonth(LocalDateTime createdAt, LocalDateTime releasedAt) {
    return createdAt == null || YearMonth.from(createdAt).equals(YearMonth.from(releasedAt));
  }
}
