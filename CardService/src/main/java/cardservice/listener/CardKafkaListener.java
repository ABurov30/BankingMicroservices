package cardservice.listener;

import cardservice.annotation.EventKey;
import cardservice.annotation.IdempotentKafkaEvent;
import cardservice.mapper.command.CardCommandMapper;
import cardservice.service.CardService;
import kafkacontracts.account.AccountCreatedEventPayload;
import kafkacontracts.account.AccountFrozenEventPayload;
import kafkacontracts.account.AccountUnfrozenEventPayload;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
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

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.account.AccountEventType).ACCOUNT_CREATED.getTopic()}"
    )
    public void handleAccountCreated(AccountCreatedEventPayload payload,
                                     @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        cardService.createCard(commandMapper.toCreateCardCommand(payload));
    }


    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.account.AccountEventType).ACCOUNT_FROZEN.getTopic()}"
    )
    public void handleAccountFrozen(AccountFrozenEventPayload payload,
                                    @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        cardService.freezeCards(commandMapper.toFreezeCardsCommand(payload));
    }

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.account.AccountEventType).ACCOUNT_UNFROZEN.getTopic()}"
    )
    public void handleAccountUnfrozen(AccountUnfrozenEventPayload payload,
                                      @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        cardService.unfreezeCards(commandMapper.toUnfreezeCardsCommand(payload));
    }
}
