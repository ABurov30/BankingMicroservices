package userservice.listener;
import kafkacontracts.auth.AuthUserBlockedEventPayload;
import kafkacontracts.auth.AuthUserCreatedEventPayload;
import kafkacontracts.auth.AuthUserRoleChangedEventPayload;
import kafkacontracts.auth.AuthUserUnlockEventPayload;
import kafkacontracts.auth.AuthUserVerifiedEventPayload;
import kafkacontracts.common.KafkaTopics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
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

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_CREATED.getTopic()}"
    )
    public void handleAuthUserCreated(AuthUserCreatedEventPayload payload) {
        userService.createUser(commandMapper.toCreateUserCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_BLOCKED.getTopic()}"
    )
    public void handleAuthUserBlocked(AuthUserBlockedEventPayload payload) {
        userService.blockUser(commandMapper.toBlockedUserCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_UNLOCK.getTopic()}"
    )
    public void handleAuthUserUnlock(AuthUserUnlockEventPayload payload) {
        userService.unlockUser(commandMapper.toUnlockUserCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_VERIFIED.getTopic()}"
    )
    public void handleAuthUserVerified(AuthUserVerifiedEventPayload payload) {
        userService.verifyUser(commandMapper.toVerifyUserCommand(payload));
    }

    @KafkaListener(
            topics = KafkaTopics.AUTH_USER_ROLE_CHANGED
    )
    public void handleAuthUserRoleChanged(AuthUserRoleChangedEventPayload payload) {
        userService.changeUserRole(commandMapper.toChangeUserRoleCommand(payload));
    }
}
