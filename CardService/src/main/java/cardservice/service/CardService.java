package cardservice.service;

import cardservice.dto.*;
import cardservice.entity.AccountOwnershipProjectionEntity;
import cardservice.entity.CardEntity;
import cardservice.entity.CardOutboxEventEntity;
import cardservice.exception.CardBlockedException;
import cardservice.exception.CardExpiredException;
import cardservice.exception.CardGenerationFailedException;
import cardservice.exception.CardNotFoundException;
import cardservice.exception.CardsNotFoundException;
import cardservice.exception.InvalidCardLimitException;
import cardservice.mapper.result.CardResultMapper;
import cardservice.repository.AccountOwnershipProjectionRepository;
import cardservice.repository.CardOutboxEventRepository;
import cardservice.repository.CardRepository;
import enums.card.CardStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import kafkacontracts.card.CardEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardService {
  private final CardRepository cardRepository;
  private final AccountOwnershipProjectionRepository accountOwnershipProjectionRepository;
  private final CardOutboxEventRepository cardOutboxEventRepository;
  private final CardResultMapper resultMapper;
  private static final int CARD_EXPIRATION_YEARS = 5;
  private static final String CARD_BIN = "400000";
  private static final int PAN_LENGTH = 16;
  private static final int ATTEMPTS_TO_GENERATE_PAN = 10;

  public CardService(
      CardRepository cardRepository,
      AccountOwnershipProjectionRepository accountOwnershipProjectionRepository,
      CardOutboxEventRepository cardOutboxEventRepository,
      CardResultMapper resultMapper) {
    this.cardRepository = cardRepository;
    this.accountOwnershipProjectionRepository = accountOwnershipProjectionRepository;
    this.cardOutboxEventRepository = cardOutboxEventRepository;
    this.resultMapper = resultMapper;
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
    if (createdCardCommand.role() != null
        && !canAccessAccount(
            createdCardCommand.accountId(),
            createdCardCommand.authUserId(),
            createdCardCommand.role())) {
      throw new CardsNotFoundException(createdCardCommand.accountId());
    }

    CardEntity cardEntity = new CardEntity();
    cardEntity.setAccountId(createdCardCommand.accountId());
    cardEntity.setPan(generateUniquePan());
    cardEntity.setCardStatus(CardStatus.ACTIVE);
    cardEntity.setDailyLimit(BigDecimal.ZERO);
    cardEntity.setMonthlyLimit(BigDecimal.ZERO);
    cardEntity.setExpiresAt(LocalDateTime.now().plusYears(CARD_EXPIRATION_YEARS));
    CardEntity savedCard = cardRepository.save(cardEntity);

    if (createdCardCommand.authUserId() != null) {
      AccountOwnershipProjectionEntity accountOwnershipProjectionEntity =
          new AccountOwnershipProjectionEntity();
      accountOwnershipProjectionEntity.setOwnerAuthUserId(createdCardCommand.authUserId());
      accountOwnershipProjectionEntity.setAccountId(createdCardCommand.accountId());
      accountOwnershipProjectionEntity.setAccountNumber(createdCardCommand.accountNumber());
      accountOwnershipProjectionRepository.save(accountOwnershipProjectionEntity);
    }

    if (createdCardCommand.accountNumber() != null) {
      createCardOutboxEvent(
          CardEventType.CARD_CREATED,
          savedCard,
          createdCardCommand.authUserId(),
          createdCardCommand.accountNumber());
    }
    return resultMapper.toCreateCardResult(savedCard);
  }

  private void changeCardStatus(CardEntity cardEntity, CardStatus cardStatus) {
    cardEntity.setCardStatus(cardStatus);
  }

  private void changeCardDailyLimit(
      CardEntity cardEntity, BigDecimal newDailyLimit, BigDecimal newMonthlyLimit) {
    BigDecimal monthlyLimit =
        newMonthlyLimit != null ? newMonthlyLimit : cardEntity.getMonthlyLimit();

    if (newDailyLimit.compareTo(monthlyLimit) > 0) {
      throw new InvalidCardLimitException("Daily limit should be less or equals monthly limit");
    }

    cardEntity.setDailyLimit(newDailyLimit);
  }

  private void changeCardMonthlyLimit(CardEntity cardEntity, BigDecimal newMonthlyLimit) {
    cardEntity.setMonthlyLimit(newMonthlyLimit);
  }

  @Transactional
  public void freezeCards(FreezeCardsCommand freezeCardsCommand) {
    List<CardEntity> cardEntityList =
        cardRepository
            .findAllByAccountId(freezeCardsCommand.accountId())
            .orElseThrow(() -> new CardsNotFoundException(freezeCardsCommand.accountId()));

    cardEntityList.forEach(
        card -> {
          if (card.getCardStatus() != CardStatus.BLOCKED) {
            changeCardStatus(card, CardStatus.FROZEN);
            createCardOutboxEvent(
                CardEventType.CARD_FROZEN,
                card,
                freezeCardsCommand.authUserId(),
                freezeCardsCommand.accountNumber());
          }
        });

    cardRepository.saveAll(cardEntityList);
  }

  @Transactional
  public void unfreezeCards(UnfreezeCardsCommand unfreezeCardsCommand) {
    List<CardEntity> cardEntityList =
        cardRepository
            .findAllByAccountId(unfreezeCardsCommand.accountId())
            .orElseThrow(() -> new CardsNotFoundException(unfreezeCardsCommand.accountId()));

    cardEntityList.forEach(
        card -> {
          if (card.getCardStatus() == CardStatus.FROZEN) {
            changeCardStatus(card, CardStatus.ACTIVE);
            createCardOutboxEvent(
                CardEventType.CARD_UNFROZEN,
                card,
                unfreezeCardsCommand.authUserId(),
                unfreezeCardsCommand.accountNumber());
          }
        });

    cardRepository.saveAll(cardEntityList);
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

    if (updateCardCommand.dailyLimit() != null) {
      this.changeCardDailyLimit(
          cardEntity, updateCardCommand.dailyLimit(), updateCardCommand.monthlyLimit());
    }

    if (updateCardCommand.monthlyLimit() != null) {
      this.changeCardMonthlyLimit(cardEntity, updateCardCommand.monthlyLimit());
    }

    CardEntity savedCard = cardRepository.save(cardEntity);

    return resultMapper.toUpdateCardResult(savedCard);
  }

  public List<GetCardResult> getCardsByAccountId(GetCardsByAccountIdCommand command) {
    List<CardEntity> cardEntityList =
        cardRepository
            .findByAccountId(command.accountId())
            .orElseThrow(() -> new CardsNotFoundException(command.accountId()));

    return cardEntityList.stream().map(resultMapper::toGetCardResult).toList();
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
}
