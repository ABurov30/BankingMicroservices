package transactionservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import enums.account.ReservationStatus;
import enums.common.Currency;
import enums.transaction.TransactionStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import transactionservice.client.AccountGrpcClient;
import transactionservice.client.CardGrpcClient;
import transactionservice.dto.CreateTransactionCommand;
import transactionservice.dto.ReservationResponseDto;
import transactionservice.dto.ReserveFudsForTransactionResponseDto;
import transactionservice.entity.TransactionEntity;
import transactionservice.exception.FundsReservationFailedException;
import transactionservice.grpc.TransactionStatusStreamRegistry;
import transactionservice.mapper.grpc.TransactionGrpcMapper;
import transactionservice.repository.TransactionRepository;

class ReservationServiceTest {
  private final AccountGrpcClient account = mock(AccountGrpcClient.class);
  private final CardGrpcClient card = mock(CardGrpcClient.class);
  private final ReservationFailureService failure = mock(ReservationFailureService.class);
  private final TransactionRepository transactions = mock(TransactionRepository.class);
  private final TransactionStatusStreamRegistry streams =
      mock(TransactionStatusStreamRegistry.class);
  private final ReservationService service =
      new ReservationService(
          account, mock(TransactionGrpcMapper.class), card, failure, transactions, streams);

  @Test
  void compensatesCardWhenAccountReservationIsRejected() {
    var transaction = transaction();
    var command = command();
    when(card.reserveLimitsForTransaction(any()))
        .thenReturn(new ReservationResponseDto(ReservationStatus.RESERVED, "Card limit reserved"));
    when(account.reserveFundsForTransaction(any()))
        .thenReturn(
            new ReserveFudsForTransactionResponseDto(
                null,
                null,
                new ReservationResponseDto(
                    ReservationStatus.FAILED, "Insufficient account funds")));
    when(transactions.saveAndFlush(any(TransactionEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    assertThatThrownBy(() -> service.reserve(command, transaction))
        .isInstanceOf(FundsReservationFailedException.class)
        .hasMessageContaining("Insufficient account funds");

    var response = ArgumentCaptor.forClass(ReservationResponseDto.class);
    verify(failure).onReservationFailed(eq(transaction), response.capture(), eq(command));
    assertThat(response.getValue().message()).contains("Insufficient account funds");
    assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.CARD_LIMIT_RESERVED);
    verify(streams).notifyStatusChanged(transaction);
    verify(transactions).saveAndFlush(transaction);
  }

  private CreateTransactionCommand command() {
    return new CreateTransactionCommand(
        UUID.randomUUID(),
        UUID.randomUUID(),
        100L,
        Currency.USD,
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID());
  }

  private TransactionEntity transaction() {
    var transaction = new TransactionEntity();
    transaction.setId(UUID.randomUUID());
    transaction.setSourceAccountId(UUID.randomUUID());
    transaction.setTargetAccountId(UUID.randomUUID());
    transaction.setMinorUnits(100L);
    transaction.setCurrency(Currency.USD);
    transaction.setStatus(TransactionStatus.CREATED);
    return transaction;
  }
}
