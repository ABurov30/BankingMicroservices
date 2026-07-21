package userservice.listener;
import kafkacontracts.auth.AuthUserCreatedEventPayload;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import userservice.mapper.UserMapper;
import userservice.service.UserService;

import java.util.UUID;

@Component
public class UserKafkaListener {

    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final UserMapper userMapper;

    public UserKafkaListener(
           ObjectMapper objectMapper,
           UserService userService,
           UserMapper userMapper
    ) {
        this.objectMapper = objectMapper;
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_CREATED.getTopic()}"
    )
    public void handleAuthUserCreated(String payload) {
        AuthUserCreatedEventPayload authUserCreatedEventPayload = objectMapper.readValue(payload, AuthUserCreatedEventPayload.class);
        userService.createUser(userMapper.toCreateUserCommand(authUserCreatedEventPayload));
    }
}
