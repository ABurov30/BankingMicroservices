package userservice.listener;

import kafkacontracts.auth.AuthUserBlockedEventPayload;
import kafkacontracts.auth.AuthUserCreatedEventPayload;
import kafkacontracts.auth.AuthUserRoleChangedEventPayload;
import kafkacontracts.auth.AuthUserUnlockEventPayload;
import kafkacontracts.auth.AuthUserVerifiedEventPayload;
import kafkacontracts.common.KafkaTopics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import userservice.annotation.EventKey;
import userservice.annotation.IdempotentKafkaEvent;
import userservice.mapper.command.UserCommandMapper;
import userservice.service.UserService;

@Component
public class UserKafkaListener {

    private final UserService userService;
    private final UserCommandMapper commandMapper;

    public UserKafkaListener(
           UserService userService,
           UserCommandMapper commandMapper
    ) {
        this.userService = userService;
        this.commandMapper = commandMapper;
    }

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_CREATED.getTopic()}"
    )
    public void handleAuthUserCreated(AuthUserCreatedEventPayload payload,
                                      @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        userService.createUser(commandMapper.toCreateUserCommand(payload));
    }

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_BLOCKED.getTopic()}"
    )
    public void handleAuthUserBlocked(AuthUserBlockedEventPayload payload,
                                      @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        userService.blockUser(commandMapper.toBlockedUserCommand(payload));
    }

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_UNLOCK.getTopic()}"
    )
    public void handleAuthUserUnlock(AuthUserUnlockEventPayload payload,
                                     @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        userService.unlockUser(commandMapper.toUnlockUserCommand(payload));
    }

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_VERIFIED.getTopic()}"
    )
    public void handleAuthUserVerified(AuthUserVerifiedEventPayload payload,
                                       @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        userService.verifyUser(commandMapper.toVerifyUserCommand(payload));
    }

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = KafkaTopics.AUTH_USER_ROLE_CHANGED
    )
    public void handleAuthUserRoleChanged(AuthUserRoleChangedEventPayload payload,
                                          @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        userService.changeUserRole(commandMapper.toChangeUserRoleCommand(payload));
    }
}
