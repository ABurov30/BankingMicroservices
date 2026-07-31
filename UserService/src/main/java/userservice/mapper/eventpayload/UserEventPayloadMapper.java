package userservice.mapper.eventpayload;

import kafkacontracts.user.UserProfileBlockedEventPayload;
import kafkacontracts.user.UserProfileCreatedEventPayload;
import kafkacontracts.user.UserProfileUnlockEventPayload;
import org.mapstruct.Mapper;

import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface UserEventPayloadMapper {
    default UserProfileCreatedEventPayload toUserProfileCreatedEventPayload(Map<String, Object> payload) {
        return UserProfileCreatedEventPayload.newBuilder()
                .setUserId(UUID.fromString(payload.get("userId").toString()))
                .setAuthUserId(UUID.fromString(payload.get("authUserId").toString()))
                .build();
    }

    default UserProfileBlockedEventPayload toUserProfileBlockedEventPayload(Map<String, Object> payload) {
        return UserProfileBlockedEventPayload.newBuilder()
                .setUserId(UUID.fromString(payload.get("userId").toString()))
                .build();
    }

    default UserProfileUnlockEventPayload toUserProfileUnlockEventPayload(Map<String, Object> payload) {
        return UserProfileUnlockEventPayload.newBuilder()
                .setUserId(UUID.fromString(payload.get("userId").toString()))
                .build();
    }
}
