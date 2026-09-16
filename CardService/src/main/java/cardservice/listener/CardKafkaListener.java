package cardservice.listener;

import cardservice.mapper.command.CardCommandMapper;
import cardservice.service.CardService;
import enums.common.Currency;
import kafkacontracts.account.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import processedevent.annotation.EventKey;
import processedevent.annotation.IdempotentKafkaEvent;

@Component
@RequiredArgsConstructor
public class CardKafkaListener {

  private final CardService cardService;
  private final CardCommandMapper commandMapper;

  @IdempotentKafkaEvent
  @KafkaListener(
      topics = "#{T(kafkacontracts.account.AccountEventType).ACCOUNT_CREATED.getTopic()}")
  public void handleAccountCreated(
      AccountCreatedEventPayload payload, @EventKey @Header("eventId") String eventId) {
    cardService.createCard(
        commandMapper.toCreateCardCommand(payload), Currency.valueOf(payload.getCurrency()));
  }

  @IdempotentKafkaEvent
  @KafkaListener(topics = "#{T(kafkacontracts.account.AccountEventType).ACCOUNT_FROZEN.getTopic()}")
  public void handleAccountFrozen(
      AccountFrozenEventPayload payload, @EventKey @Header("eventId") String eventId) {
    cardService.freezeCards(commandMapper.toFreezeCardsCommand(payload));
  }

  @IdempotentKafkaEvent
  @KafkaListener(
      topics = "#{T(kafkacontracts.account.AccountEventType).ACCOUNT_UNFROZEN.getTopic()}")
  public void handleAccountUnfrozen(
      AccountUnfrozenEventPayload payload, @EventKey @Header("eventId") String eventId) {
    cardService.unfreezeCards(commandMapper.toUnfreezeCardsCommand(payload));
  }

  @IdempotentKafkaEvent
  @KafkaListener(
      topics = "#{T(kafkacontracts.account.AccountEventType).TRANSACTION_COMPENSATED.getTopic()}")
  public void handleTransactionCompensated(
      TransactionCompensatedEventPayload payload, @EventKey @Header("eventId") String eventId) {
    cardService.compensateLimitsForTransaction(
        commandMapper.toCompensateLimitsForTransactionCommand(payload));
  }

  @IdempotentKafkaEvent
  @KafkaListener(
      topics = "#{T(kafkacontracts.account.AccountEventType).TRANSACTION_COMPLETED.getTopic()}")
  public void handleTransactionCompleted(
      TransactionCompletedEventPayload payload, @EventKey @Header("eventId") String eventId) {
    cardService.markLimitReservationAsReleased(
        commandMapper.toMarkLimitReservationAsReleasedCommand(payload));
  }

  @IdempotentKafkaEvent
  @KafkaListener(
      topics =
          "#{T(kafkacontracts.transaction.TransactionEventType)"
              + ".TRANSACTION_CARD_LIMIT_HOLD_COMPENSATION.getTopic()}")
  public void handleTransactionCardLimitHoldCompensation(
      TransactionCardLimitHoldCompensationEventPayload payload,
      @EventKey @Header("eventId") String eventId) {
    cardService.compensateLimitsForTransaction(
        commandMapper.toCompensateLimitsForTransactionCommand(payload));
  }
}
