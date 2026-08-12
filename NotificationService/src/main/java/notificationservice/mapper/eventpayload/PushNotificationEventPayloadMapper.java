package notificationservice.mapper.eventpayload;

import kafkacontracts.account.NotificationCreatedEventPayload;
import org.mapstruct.Mapper;

import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface PushNotificationEventPayloadMapper {

    default UUID uuid(Object value) {
        return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
    }

    default NotificationCreatedEventPayload toPushNotificationCreatedEventPayload(Map<String, Object> value) {
        return NotificationCreatedEventPayload.newBuilder()
                .setAuthUserId(uuid(value.get("authUserId")))
                .setTitle(value.get("title").toString())
                .setBody(value.get("body").toString())
                .setType(value.get("type").toString())
                .build();
    }
}
