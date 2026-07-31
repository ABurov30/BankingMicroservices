package cardservice.listener;

import cardservice.mapper.command.CardCommandMapper;
import cardservice.service.CardService;
import kafkacontracts.account.AccountCreatedEventPayload;
import kafkacontracts.account.AccountFrozenEventPayload;
import kafkacontracts.account.AccountUnfrozenEventPayload;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CardKafkaListener {

    private final CardService cardService;
    private final CardCommandMapper commandMapper;

    public CardKafkaListener(
            CardService cardService,
            CardCommandMapper commandMapper
    ) {
        this.cardService = cardService;
        this.commandMapper = commandMapper;
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.account.AccountEventType).ACCOUNT_CREATED.getTopic()}"
    )
    public void handleAccountCreated(AccountCreatedEventPayload payload) {
        cardService.createCard(commandMapper.toCreateCardCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.account.AccountEventType).ACCOUNT_FROZEN.getTopic()}"
    )
    public void handleAccountFrozen(AccountFrozenEventPayload payload) {
        cardService.freezeCards(commandMapper.toFreezeCardsCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.account.AccountEventType).ACCOUNT_UNFROZEN.getTopic()}"
    )
    public void handleAccountUnfrozen(AccountUnfrozenEventPayload payload) {
        cardService.unfreezeCards(commandMapper.toUnfreezeCardsCommand(payload));
    }
}
