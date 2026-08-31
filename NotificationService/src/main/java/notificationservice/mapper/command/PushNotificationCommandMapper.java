package notificationservice.mapper.command;

import enums.common.Currency;
import enums.transaction.TransactionDirection;
import java.math.BigDecimal;
import kafkacontracts.account.*;
import kafkacontracts.card.CardCreatedEventPayload;
import kafkacontracts.card.CardFrozenEventPayload;
import kafkacontracts.card.CardUnfrozenEventPayload;
import notificationservice.dto.AccountPushNotificationPayload;
import notificationservice.dto.CardPushNotificationPayload;
import notificationservice.dto.CreatePushNotificationCommand;
import notificationservice.dto.TransactionPushNotificationPayload;
import notificationservice.enums.push.PushNotificationType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PushNotificationCommandMapper {
  default AccountPushNotificationPayload toAccountPushNotificationPayload(
      AccountCreatedEventPayload payload) {
    return new AccountPushNotificationPayload(payload.getAccountId(), payload.getAccountNumber());
  }

  default AccountPushNotificationPayload toAccountPushNotificationPayload(
      AccountFrozenEventPayload payload) {
    return new AccountPushNotificationPayload(payload.getAccountId(), payload.getAccountNumber());
  }

  default AccountPushNotificationPayload toAccountPushNotificationPayload(
      AccountUnfrozenEventPayload payload) {
    return new AccountPushNotificationPayload(payload.getAccountId(), payload.getAccountNumber());
  }

  default CreatePushNotificationCommand toCreatePushNotificationCommand(
      AccountCreatedEventPayload payload) {
    return new CreatePushNotificationCommand(
        (AccountPushNotificationPayload) toAccountPushNotificationPayload(payload),
        PushNotificationType.ACCOUNT_CREATED,
        payload.getAuthUserId());
  }

  default CreatePushNotificationCommand toCreatePushNotificationCommand(
      AccountFrozenEventPayload payload) {
    return new CreatePushNotificationCommand(
        (AccountPushNotificationPayload) toAccountPushNotificationPayload(payload),
        PushNotificationType.ACCOUNT_FROZEN,
        payload.getAuthUserId());
  }

  default CreatePushNotificationCommand toCreatePushNotificationCommand(
      AccountUnfrozenEventPayload payload) {
    return new CreatePushNotificationCommand(
        (AccountPushNotificationPayload) toAccountPushNotificationPayload(payload),
        PushNotificationType.ACCOUNT_UNFROZEN,
        payload.getAuthUserId());
  }

  default CreatePushNotificationCommand toCreatePushNotificationCommand(
      CardCreatedEventPayload payload) {
    return new CreatePushNotificationCommand(
        toCardPushNotificationPayload(payload),
        PushNotificationType.CARD_CREATED,
        payload.getAuthUserId());
  }

  default CreatePushNotificationCommand toCreatePushNotificationCommand(
      CardFrozenEventPayload payload) {
    return new CreatePushNotificationCommand(
        toCardPushNotificationPayload(payload),
        PushNotificationType.CARD_FROZEN,
        payload.getAuthUserId());
  }

  default CreatePushNotificationCommand toCreatePushNotificationCommand(
      CardUnfrozenEventPayload payload) {
    return new CreatePushNotificationCommand(
        toCardPushNotificationPayload(payload),
        PushNotificationType.CARD_UNFROZEN,
        payload.getAuthUserId());
  }

  default CreatePushNotificationCommand toCreatePushNotificationCommand(
      TransactionFailedEventPayload payload) {
    return new CreatePushNotificationCommand(
        toTransactionPushNotificationPayload(payload),
        PushNotificationType.TRANSACTION_FAILED,
        payload.getAuthUserId());
  }

  default CreatePushNotificationCommand toCreatePushNotificationCommand(
      TransactionCompletedEventPayload payload, TransactionDirection transactionDirection) {
    return new CreatePushNotificationCommand(
        toTransactionPushNotificationPayload(payload),
        TransactionDirection.RECIPIENT == transactionDirection
            ? PushNotificationType.TRANSACTION_RECEIVED
            : PushNotificationType.TRANSACTION_COMPLETED,
        payload.getAuthUserId());
  }

  default CardPushNotificationPayload toCardPushNotificationPayload(
      CardCreatedEventPayload payload) {
    return new CardPushNotificationPayload(
        payload.getAccountId(), payload.getAccountNumber(), payload.getCardNumber());
  }

  default CardPushNotificationPayload toCardPushNotificationPayload(
      CardFrozenEventPayload payload) {
    return new CardPushNotificationPayload(
        payload.getAccountId(), payload.getAccountNumber(), payload.getCardNumber());
  }

  default CardPushNotificationPayload toCardPushNotificationPayload(
      CardUnfrozenEventPayload payload) {
    return new CardPushNotificationPayload(
        payload.getAccountId(), payload.getAccountNumber(), payload.getCardNumber());
  }

  default TransactionPushNotificationPayload toTransactionPushNotificationPayload(
      TransactionFailedEventPayload payload) {

    var currency = Currency.valueOf(payload.getCurrency());
    return new TransactionPushNotificationPayload(
        null, BigDecimal.valueOf(payload.getAmountMinorUnits(), currency.getMinorUnit()), currency);
  }

  default TransactionPushNotificationPayload toTransactionPushNotificationPayload(
      TransactionCompletedEventPayload payload) {
    var currency = Currency.valueOf(payload.getCurrency());
    return new TransactionPushNotificationPayload(
        payload.getAccountNumber(),
        BigDecimal.valueOf(payload.getAmountMinorUnits(), currency.getMinorUnit()),
        currency);
  }
}
