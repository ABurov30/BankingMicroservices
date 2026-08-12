package cardservice.mapper.eventpayload;

import java.util.Map;
import java.util.UUID;
import kafkacontracts.card.CardCreatedEventPayload;
import kafkacontracts.card.CardFrozenEventPayload;
import kafkacontracts.card.CardUnfrozenEventPayload;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CardEventPayloadMapper {
  default UUID uuid(Object value) {
    return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
  }

  default CardCreatedEventPayload toCardCreatedEventPayload(Map<String, Object> payload) {
    return CardCreatedEventPayload.newBuilder()
        .setAuthUserId(uuid(payload.get("authUserId")))
        .setAccountId(uuid(payload.get("accountId")))
        .setAccountNumber(payload.get("accountNumber").toString())
        .setCardId(uuid(payload.get("cardId")))
        .setCardNumber(payload.get("cardNumber").toString())
        .build();
  }

  default CardFrozenEventPayload toCardFrozenEventPayload(Map<String, Object> payload) {
    return CardFrozenEventPayload.newBuilder()
        .setAuthUserId(uuid(payload.get("authUserId")))
        .setAccountId(uuid(payload.get("accountId")))
        .setAccountNumber(payload.get("accountNumber").toString())
        .setCardId(uuid(payload.get("cardId")))
        .setCardNumber(payload.get("cardNumber").toString())
        .build();
  }

  default CardUnfrozenEventPayload toCardUnfrozenEventPayload(Map<String, Object> payload) {
    return CardUnfrozenEventPayload.newBuilder()
        .setAuthUserId(uuid(payload.get("authUserId")))
        .setAccountId(uuid(payload.get("accountId")))
        .setAccountNumber(payload.get("accountNumber").toString())
        .setCardId(uuid(payload.get("cardId")))
        .setCardNumber(payload.get("cardNumber").toString())
        .build();
  }
}
