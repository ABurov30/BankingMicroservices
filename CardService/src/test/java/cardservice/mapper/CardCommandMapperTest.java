package cardservice.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import card.contract.v1.*;
import cardservice.mapper.command.CardCommandMapperImpl;
import enums.auth.Roles;
import enums.card.CardStatus;
import enums.common.Currency;
import java.util.UUID;
import kafkacontracts.account.AccountCreatedEventPayload;
import kafkacontracts.account.TransactionCompensatedEventPayload;
import kafkacontracts.account.TransactionCompletedEventPayload;
import org.junit.jupiter.api.Test;

class CardCommandMapperTest {
  @Test
  void mapsGrpcRequestsAndAccountEvents() {
    UUID account = UUID.randomUUID();
    UUID user = UUID.randomUUID();
    UUID transaction = UUID.randomUUID();
    var mapper = new CardCommandMapperImpl();
    assertThat(
            mapper
                .toGetCardsByAccountIdsCommand(
                    GetCardByAccountIdsGrpcRequest.newBuilder()
                        .addAccountId(account.toString())
                        .setAuthUserId(user.toString())
                        .setRole(Roles.USER.name())
                        .build())
                .accountIds())
        .containsExactly(account);
    assertThat(
            mapper
                .toCreateCardCommand(
                    CreateCardGrpcRequest.newBuilder()
                        .setAccountId(account.toString())
                        .setAuthUserId(user.toString())
                        .setCurrency(Currency.USD.name())
                        .setRole(Roles.USER.name())
                        .build())
                .currency())
        .isEqualTo(Currency.USD);
    assertThat(
            mapper
                .toUpdateCardCommand(
                    UpdateCardGrpcRequest.newBuilder()
                        .setCardId(UUID.randomUUID().toString())
                        .setAuthUserId(user.toString())
                        .setStatus(CardStatus.ACTIVE.name())
                        .setRole(Roles.USER.name())
                        .setDailyLimitMinorUnits(1)
                        .build())
                .status())
        .isEqualTo(CardStatus.ACTIVE);
    assertThat(
            mapper
                .toGetCardsByAccountIdCommand(
                    GetCardByAccountIdGrpcRequest.newBuilder()
                        .setAccountId(account.toString())
                        .setAuthUserId(user.toString())
                        .setRole(Roles.USER.name())
                        .build())
                .role())
        .isEqualTo(Roles.USER);
    assertThat(
            mapper
                .toReserveLimitsForTransactionCommand(
                    ReserveLimitsForTransactionGrpcRequest.newBuilder()
                        .setSourceCardId(UUID.randomUUID().toString())
                        .setTransactionId(transaction.toString())
                        .setSourceAuthUserId(user.toString())
                        .setCurrency(Currency.USD.name())
                        .setMinorUnits(1)
                        .build())
                .currency())
        .isEqualTo(Currency.USD);
    var accountEvent =
        AccountCreatedEventPayload.newBuilder()
            .setAccountId(account)
            .setAuthUserId(user)
            .setAccountNumber("ACC")
            .setCurrency(Currency.USD.name())
            .build();
    assertThat(mapper.toCreateCardCommand(accountEvent).accountId()).isEqualTo(account);
    assertThat(
            mapper
                .toCompensateLimitsForTransactionCommand(
                    TransactionCompensatedEventPayload.newBuilder()
                        .setTransactionId(transaction)
                        .build())
                .transactionId())
        .isEqualTo(transaction);
    assertThat(
            mapper
                .toMarkLimitReservationAsReleasedCommand(
                    TransactionCompletedEventPayload.newBuilder()
                        .setTransactionId(transaction)
                        .setAccountNumber("ACC")
                        .setAmountMinorUnits(1)
                        .setCurrency(Currency.USD.name())
                        .setAuthUserId(user)
                        .build())
                .transactionId())
        .isEqualTo(transaction);
  }
}
