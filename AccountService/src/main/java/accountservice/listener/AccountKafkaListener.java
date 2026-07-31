package accountservice.listener;

import accountservice.mapper.command.AccountCommandMapper;
import accountservice.service.AccountService;
import kafkacontracts.user.UserProfileBlockedEventPayload;
import kafkacontracts.user.UserProfileCreatedEventPayload;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AccountKafkaListener {

    private final AccountService accountService;
    private final AccountCommandMapper commandMapper;

    public AccountKafkaListener (
        AccountService accountService,
        AccountCommandMapper commandMapper
    ) {
        this.accountService = accountService;
        this.commandMapper = commandMapper;
    }


    @KafkaListener(
            topics = "#{T(kafkacontracts.user.UserEventType).USER_PROFILE_CREATED.getTopic()}"
    )
    public void handleUserProfileCreated(UserProfileCreatedEventPayload payload) {
        accountService.createAccount(commandMapper.toCreateAccountCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.user.UserEventType).USER_PROFILE_BLOCKED.getTopic()}"
    )
    public void handleUserProfileBlocked(UserProfileBlockedEventPayload payload) {
        accountService.freezeAccountByUserId(commandMapper.toFreezeAccountsByUserIdCommand(payload));
    }
}
