package cardservice.service;

import cardservice.repository.CardOutboxEventRepository;
import java.util.Map;
import java.util.UUID;
import kafkacontracts.card.CardEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardOutboxService {
  private final CardOutboxEventRepository repository;

  public void saveCardOutboxEvent(UUID id, CardEventType eventType, Map<String, Object> payload) {
    var event = CardOutboxEventFactory.create(id, eventType);
    event.setPayload(payload);
    repository.save(event);
  }
}
