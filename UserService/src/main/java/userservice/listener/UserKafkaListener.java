package userservice.listener;
import kafkacontracts.auth.AuthUserBlockedEventPayload;
import kafkacontracts.auth.AuthUserCreatedEventPayload;
import kafkacontracts.auth.AuthUserUnlockEventPayload;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import userservice.mapper.UserMapper;
import userservice.service.UserService;

@Component
public class UserKafkaListener {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserKafkaListener(
           UserService userService,
           UserMapper userMapper
    ) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_CREATED.getTopic()}"
    )
    public void handleAuthUserCreated(AuthUserCreatedEventPayload payload) {
        userService.createUser(userMapper.toCreateUserCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_BLOCKED.getTopic()}"
    )
    public void handleAuthUserBlocked(AuthUserBlockedEventPayload payload) {
        userService.blockUser(userMapper.toBlockedUserCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_UNLOCK.getTopic()}"
    )
    public void handleAuthUserUnlock(AuthUserUnlockEventPayload payload) {
        userService.unlockUser(userMapper.toUnlockUserCommand(payload));
    }
}
