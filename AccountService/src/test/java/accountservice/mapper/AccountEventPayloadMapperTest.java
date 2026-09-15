package accountservice.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import accountservice.mapper.eventpayload.AccountEventPayloadMapperImpl;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountEventPayloadMapperTest {
  @Test
  void mapsAllPayloadShapesAndNumericValues() {
    UUID account = UUID.randomUUID();
    UUID user = UUID.randomUUID();
    UUID transaction = UUID.randomUUID();
    var mapper = new AccountEventPayloadMapperImpl();
    var base =
        Map.<String, Object>of(
            "accountId", account, "authUserId", user, "accountNumber", "ACC", "currency", "USD");
    assertThat(mapper.toAccountCreatedEventPayload(base).getAccountId()).isEqualTo(account);
    assertThat(mapper.toAccountFrozenEventPayload(base).getAuthUserId()).isEqualTo(user);
    assertThat(mapper.toAccountUnfrozenEventPayload(base).getAccountNumber()).isEqualTo("ACC");
    var tx =
        Map.<String, Object>of(
            "accountNumber",
            "ACC",
            "transactionId",
            transaction,
            "amountMinorUnits",
            12,
            "currency",
            "USD",
            "authUserId",
            user);
    assertThat(mapper.toTransactionCompletedEventPayload(tx).getAmountMinorUnits()).isEqualTo(12);
    assertThat(
            mapper
                .toTransactionCompensatedEventPayload(Map.of("transactionId", transaction))
                .getTransactionId())
        .isEqualTo(transaction);
    assertThat(
            mapper
                .toAccountHoldReleasedByTimeEventPayload(Map.of("transactionId", transaction))
                .getTransactionId())
        .isEqualTo(transaction);
    assertThatThrownBy(
            () ->
                mapper.toTransactionCompletedEventPayload(
                    Map.of(
                        "accountNumber",
                        "ACC",
                        "transactionId",
                        transaction,
                        "currency",
                        "USD",
                        "authUserId",
                        user)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
