package cardservice.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import cardservice.mapper.eventpayload.CardEventPayloadMapperImpl;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CardEventPayloadMapperTest {
  @Test
  void mapsCardPayloadsWithUuidAndStringValues() {
    UUID user = UUID.randomUUID();
    UUID account = UUID.randomUUID();
    UUID card = UUID.randomUUID();
    var payload =
        Map.<String, Object>of(
            "authUserId",
            user.toString(),
            "accountId",
            account,
            "accountNumber",
            "ACC",
            "cardId",
            card.toString(),
            "cardNumber",
            "CARD");
    var mapper = new CardEventPayloadMapperImpl();
    assertThat(mapper.toCardCreatedEventPayload(payload).getCardId()).isEqualTo(card);
    assertThat(mapper.toCardFrozenEventPayload(payload).getAccountId()).isEqualTo(account);
    assertThat(mapper.toCardUnfrozenEventPayload(payload).getAuthUserId()).isEqualTo(user);
    UUID transaction = UUID.randomUUID();
    assertThat(
            mapper
                .toCardLimitHoldReleasedByTimeEventPayload(Map.of("transactionId", transaction))
                .getTransactionId())
        .isEqualTo(transaction);
  }
}
