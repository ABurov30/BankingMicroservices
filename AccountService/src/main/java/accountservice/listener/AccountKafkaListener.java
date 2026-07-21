package accountservice.listener;

import accountservice.mapper.AccountMapper;
import accountservice.service.AccountService;
import kafkacontracts.user.UserProfileCreatedEventPayload;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class AccountKafkaListener {

    private final ObjectMapper objectMapper;
    private final AccountService accountService;
    private final AccountMapper accountMapper;

    public AccountKafkaListener (
        ObjectMapper objectMapper,
        AccountService accountService,
        AccountMapper accountMapper
    ) {
        this.objectMapper = objectMapper;
        this.accountService = accountService;
        this.accountMapper = accountMapper;
    }


    @KafkaListener(
            topics = "#{T(kafkacontracts.user.UserEventType).USER_PROFILE_CREATED.getTopic()}"
    )
    public void handleUserProfileCreated(String payload) {
        UserProfileCreatedEventPayload userProfileCreatedEventPayload = objectMapper.readValue(payload, UserProfileCreatedEventPayload.class);
        accountService.createAccount(accountMapper.toCreateAccountCommand(userProfileCreatedEventPayload));
    }
}
