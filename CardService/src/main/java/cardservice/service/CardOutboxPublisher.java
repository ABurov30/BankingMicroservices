package cardservice.service;

import cardservice.entity.CardOutboxEventEntity;
import cardservice.mapper.eventpayload.CardEventPayloadMapper;
import cardservice.repository.CardOutboxEventRepository;
import java.util.List;
import java.util.Map;
import kafkacontracts.card.CardEventType;
import org.apache.avro.specific.SpecificRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import outboxsupport.KafkaOnSentHandler;
import outboxsupport.OutboxEventStatus;

@Service
public class CardOutboxPublisher implements KafkaOnSentHandler {
  private static final Logger log = LoggerFactory.getLogger(CardOutboxPublisher.class);
  private final CardOutboxEventRepository cardOutboxEventRepository;
  private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;
  private final CardEventPayloadMapper eventPayloadMapper;

  public CardOutboxPublisher(
      CardOutboxEventRepository cardOutboxEventRepository,
      KafkaTemplate<String, SpecificRecord> kafkaTemplate,
      CardEventPayloadMapper eventPayloadMapper) {
    this.cardOutboxEventRepository = cardOutboxEventRepository;
    this.kafkaTemplate = kafkaTemplate;
    this.eventPayloadMapper = eventPayloadMapper;
  }

  @Scheduled(fixedDelay = 5000)
  @Transactional
  public void publishPendingEvents() {
    List<CardOutboxEventEntity> events =
        cardOutboxEventRepository.findTop50ByOutboxEventStatusOrderByCreatedAtAsc(
            OutboxEventStatus.PENDING);

    for (CardOutboxEventEntity event : events) {
      try {
        SpecificRecord payload =
            extractPayload(CardEventType.valueOf(event.getEventType()), event.getPayload());

        kafkaTemplate
            .send(event.getTopic(), event.getEventKey(), payload)
            .whenComplete(
                (result, ex) -> {
                  if (ex == null) {
                    onPublish(event.getId(), cardOutboxEventRepository);
                  } else {
                    onFailed(event.getId(), ex, cardOutboxEventRepository);
                  }
                });
      } catch (Exception e) {
        log.error(
            "Unable to publish card outbox event: eventId={}, eventType={}",
            event.getId(),
            event.getEventType(),
            e);
        onFailed(event.getId(), e, cardOutboxEventRepository);
      }
    }
  }

  private SpecificRecord extractPayload(CardEventType eventType, Map<String, Object> payload) {
    return switch (eventType) {
      case CARD_CREATED -> eventPayloadMapper.toCardCreatedEventPayload(payload);
      case CARD_FROZEN -> eventPayloadMapper.toCardFrozenEventPayload(payload);
      case CARD_UNFROZEN -> eventPayloadMapper.toCardUnfrozenEventPayload(payload);
    };
  }
}
