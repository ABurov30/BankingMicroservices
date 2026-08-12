package accountservice.service;

import accountservice.entity.AccountOutboxEventEntity;
import accountservice.mapper.eventpayload.AccountEventPayloadMapper;
import accountservice.repository.AccountOutboxEventRepository;
import java.util.List;
import java.util.Map;
import kafkacontracts.account.AccountEventType;
import org.apache.avro.specific.SpecificRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import outboxsupport.KafkaOnSentHandler;
import outboxsupport.OutboxEventStatus;

@Service
public class AccountOutboxPublisher implements KafkaOnSentHandler {
  public static final String TRANSACTION_NOTIFICATION_DIRECTION_HEADER =
      "transaction-notification-direction";
  private static final Logger log = LoggerFactory.getLogger(AccountOutboxPublisher.class);
  private final AccountOutboxEventRepository accountOutboxEventRepository;
  private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;
  private final AccountEventPayloadMapper eventPayloadMapper;

  public AccountOutboxPublisher(
      AccountOutboxEventRepository accountOutboxEventRepository,
      KafkaTemplate<String, SpecificRecord> kafkaTemplate,
      AccountEventPayloadMapper eventPayloadMapper) {
    this.accountOutboxEventRepository = accountOutboxEventRepository;
    this.kafkaTemplate = kafkaTemplate;
    this.eventPayloadMapper = eventPayloadMapper;
  }

  @Scheduled(fixedDelay = 5000)
  @Transactional
  public void publishPendingEvents() {
    List<AccountOutboxEventEntity> eventEntityList =
        accountOutboxEventRepository.findTop50ByOutboxEventStatusOrderByCreatedAtAsc(
            OutboxEventStatus.PENDING);

    for (AccountOutboxEventEntity event : eventEntityList) {
      try {
        SpecificRecord payload =
            extractPayload(AccountEventType.valueOf(event.getEventType()), event.getPayload());

        var messageBuilder =
            MessageBuilder.withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, event.getTopic())
                .setHeader(KafkaHeaders.KEY, event.getEventKey());

        if (event.getPayload().containsKey("transactionDirection")) {
          messageBuilder.setHeader(
              TRANSACTION_NOTIFICATION_DIRECTION_HEADER,
              event.getPayload().get("transactionDirection").toString());
        }

        kafkaTemplate
            .send(messageBuilder.build())
            .whenComplete(
                (result, ex) -> {
                  if (ex == null) {
                    onPublish(event.getId(), accountOutboxEventRepository);
                  } else {
                    onFailed(event.getId(), ex, accountOutboxEventRepository);
                  }
                });
      } catch (Exception e) {
        log.error(
            "Unable to publish account outbox event: eventId={}, eventType={}",
            event.getId(),
            event.getEventType(),
            e);
        onFailed(event.getId(), e, accountOutboxEventRepository);
      }
    }
  }

  private SpecificRecord extractPayload(AccountEventType eventType, Map<String, Object> payload) {
    return switch (eventType) {
      case ACCOUNT_CREATED -> eventPayloadMapper.toAccountCreatedEventPayload(payload);
      case ACCOUNT_FROZEN -> eventPayloadMapper.toAccountFrozenEventPayload(payload);
      case ACCOUNT_UNFROZEN -> eventPayloadMapper.toAccountUnfrozenEventPayload(payload);
      case TRANSACTION_COMPENSATED ->
          eventPayloadMapper.toTransactionCompensatedEventPayload(payload);
      case TRANSACTION_COMPLETED -> eventPayloadMapper.toTransactionCompletedEventPayload(payload);
    };
  }
}
