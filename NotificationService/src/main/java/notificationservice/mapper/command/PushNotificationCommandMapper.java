package notificationservice.mapper.command;

import enums.transaction.TransactionDirection;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import kafkacontracts.account.*;
import kafkacontracts.card.CardCreatedEventPayload;
import kafkacontracts.card.CardFrozenEventPayload;
import kafkacontracts.card.CardUnfrozenEventPayload;
import notificationservice.dto.AccountPushNotificationPayload;
import notificationservice.dto.CardPushNotificationPayload;
import notificationservice.dto.CreatePushNotificationCommand;
import notificationservice.dto.TransactionPushNotificationPayload;
import notificationservice.enums.push.PushNotificationType;
import org.apache.avro.Conversions;
import org.apache.avro.Schema;
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
      TransactionCompletedEventPayload payload) {
    return toCreatePushNotificationCommand(payload, null);
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
    return new TransactionPushNotificationPayload(null, toBigDecimal(payload.getAmount()));
  }

  default TransactionPushNotificationPayload toTransactionPushNotificationPayload(
      TransactionCompletedEventPayload payload) {
    return new TransactionPushNotificationPayload(
        payload.getAccountNumber(), toBigDecimal(payload.getAmount()));
  }

  private BigDecimal toBigDecimal(ByteBuffer amount) {
    Schema schema = TransactionFailedEventPayload.getClassSchema().getField("amount").schema();

    return new Conversions.DecimalConversion()
        .fromBytes(amount.duplicate(), schema, schema.getLogicalType());
  }
}
