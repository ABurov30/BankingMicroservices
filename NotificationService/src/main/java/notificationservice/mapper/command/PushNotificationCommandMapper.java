package notificationservice.mapper.command;

import kafkacontracts.account.AccountCreatedEventPayload;
import kafkacontracts.account.AccountFrozenEventPayload;
import notificationservice.dto.CreatePushNotificationCommand;
import notificationservice.enums.push.PushNotificationType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PushNotificationCommandMapper {
    default CreatePushNotificationCommand toCreatePushNotificationCommand(AccountCreatedEventPayload payload) {
        return new CreatePushNotificationCommand(payload.getAuthUserId(), payload.getAccountId(), PushNotificationType.ACCOUNT_CREATED);
    }
}
