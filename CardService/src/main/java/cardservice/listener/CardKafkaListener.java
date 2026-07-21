package cardservice.listener;

import cardservice.mapper.CardMapper;
import cardservice.service.CardService;
import kafkacontracts.account.AccountCreatedEventPayload;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class CardKafkaListener {

    private final ObjectMapper objectMapper;
    private final CardService cardService;
    private final CardMapper cardMapper;

    public CardKafkaListener(
            ObjectMapper objectMapper,
            CardService cardService,
            CardMapper cardMapper
    ) {
        this.objectMapper = objectMapper;
        this.cardService = cardService;
        this.cardMapper = cardMapper;
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AccountEventType).ACCOUNT_USER_CREATED.getTopic()}"
    )
    public void handleAccountCreated(String payload) {
        AccountCreatedEventPayload accountCreatedEventPayload= objectMapper.readValue(payload, AccountCreatedEventPayload.class);
        cardService.createCard(cardMapper.toCreateCardCommand(accountCreatedEventPayload));
    }
}
