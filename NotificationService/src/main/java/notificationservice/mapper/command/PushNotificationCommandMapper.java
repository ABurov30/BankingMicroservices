package notificationservice.mapper.command;

import kafkacontracts.account.AccountCreatedEventPayload;
import kafkacontracts.account.AccountFrozenEventPayload;
import kafkacontracts.account.AccountUnfrozenEventPayload;
import kafkacontracts.card.CardCreatedEventPayload;
import kafkacontracts.card.CardFrozenEventPayload;
import kafkacontracts.card.CardUnfrozenEventPayload;
import notificationservice.dto.CreatePushNotificationCommand;
import notificationservice.enums.push.PushNotificationType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PushNotificationCommandMapper {
    default CreatePushNotificationCommand toCreatePushNotificationCommand(AccountCreatedEventPayload payload) {
        return new CreatePushNotificationCommand(
                payload.getAuthUserId(),
                payload.getAccountId(),
                payload.getAccountNumber(),
                null,
                PushNotificationType.ACCOUNT_CREATED
        );
    }

    default CreatePushNotificationCommand toCreatePushNotificationCommand(AccountFrozenEventPayload payload) {
        return new CreatePushNotificationCommand(
                payload.getAuthUserId(),
                payload.getAccountId(),
                payload.getAccountNumber(),
                null,
                PushNotificationType.ACCOUNT_FROZEN
        );
    }

    default CreatePushNotificationCommand toCreatePushNotificationCommand(AccountUnfrozenEventPayload payload) {
        return new CreatePushNotificationCommand(
                payload.getAuthUserId(),
                payload.getAccountId(),
                payload.getAccountNumber(),
                null,
                PushNotificationType.ACCOUNT_UNFROZEN
        );
    }

    default CreatePushNotificationCommand toCreatePushNotificationCommand(CardCreatedEventPayload payload) {
        return new CreatePushNotificationCommand(
                payload.getAuthUserId(),
                payload.getAccountId(),
                payload.getAccountNumber(),
                payload.getCardNumber(),
                PushNotificationType.CARD_CREATED
        );
    }

    default CreatePushNotificationCommand toCreatePushNotificationCommand(CardFrozenEventPayload payload) {
        return new CreatePushNotificationCommand(
                payload.getAuthUserId(),
                payload.getAccountId(),
                payload.getAccountNumber(),
                payload.getCardNumber(),
                PushNotificationType.CARD_FROZEN
        );
    }

    default CreatePushNotificationCommand toCreatePushNotificationCommand(CardUnfrozenEventPayload payload) {
        return new CreatePushNotificationCommand(
                payload.getAuthUserId(),
                payload.getAccountId(),
                payload.getAccountNumber(),
                payload.getCardNumber(),
                PushNotificationType.CARD_UNFROZEN
        );
    }
}
