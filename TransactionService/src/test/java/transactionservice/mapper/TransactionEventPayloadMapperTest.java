package transactionservice.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import transactionservice.mapper.eventpayload.TransactionEventPayloadMapperImpl;

class TransactionEventPayloadMapperTest {
  @Test
  void mapsFailureAndFundsRequestedPayloads() {
    UUID user = UUID.randomUUID();
    UUID transaction = UUID.randomUUID();
    var mapper = new TransactionEventPayloadMapperImpl();
    var failed =
        Map.<String, Object>of("authUserId", user, "amountMinorUnits", "25", "currency", "EUR");
    assertThat(mapper.toTransactionFailedEventPayload(failed).getAmountMinorUnits()).isEqualTo(25);
    var requested =
        Map.<String, Object>of(
            "transactionId", transaction, "authUserId", user, "targetAccountId", UUID.randomUUID());
    assertThat(mapper.toTransactionFundsRequestedEventPayload(requested).getTransactionId())
        .isEqualTo(transaction);
    assertThatThrownBy(
            () ->
                mapper.toTransactionFailedEventPayload(
                    Map.of("authUserId", user, "currency", "EUR")))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
