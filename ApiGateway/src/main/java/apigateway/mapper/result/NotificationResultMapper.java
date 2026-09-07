package apigateway.mapper.result;

import apigateway.dto.response.notification.NotificationResponseDto;
import kafkacontracts.account.NotificationCreatedEventPayload;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationResultMapper {
  default NotificationResponseDto toNotificationResponseDto(
      NotificationCreatedEventPayload payload) {
    return new NotificationResponseDto(payload.getTitle(), payload.getBody(), payload.getType());
  }
}
