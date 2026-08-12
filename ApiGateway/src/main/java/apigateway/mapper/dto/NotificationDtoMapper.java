package apigateway.mapper.dto;

import apigateway.dto.notification.NotificationResponseDto;
import kafkacontracts.account.NotificationCreatedEventPayload;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationDtoMapper {
    default NotificationResponseDto toNotificationResponseDto (NotificationCreatedEventPayload payload) {
        return  new NotificationResponseDto(
                payload.getTitle(),
                payload.getBody(),
                payload.getType()
        );
    }
}
