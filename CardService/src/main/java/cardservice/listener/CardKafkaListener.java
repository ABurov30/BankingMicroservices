package cardservice.listener;

import cardservice.mapper.CardMapper;
import cardservice.service.CardService;
import kafkacontracts.account.AccountCreatedEventPayload;
import kafkacontracts.account.AccountFrozenEventPayload;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CardKafkaListener {

    private final CardService cardService;
    private final CardMapper cardMapper;

    public CardKafkaListener(
            CardService cardService,
            CardMapper cardMapper
    ) {
        this.cardService = cardService;
        this.cardMapper = cardMapper;
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.account.AccountEventType).ACCOUNT_CREATED.getTopic()}"
    )
    public void handleAccountCreated(AccountCreatedEventPayload payload) {
        cardService.createCard(cardMapper.toCreateCardCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.account.AccountEventType).ACCOUNT_FROZEN.getTopic()}"
    )
    public void handleAccountFrozen(AccountFrozenEventPayload payload) {
        cardService.freezeCards(cardMapper.toFreezeCardsCommand(payload));
    }
}
