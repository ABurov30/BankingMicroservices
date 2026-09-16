package transactionservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import enums.common.Currency;
import enums.transaction.TransactionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import transactionservice.dto.CreateTransactionCommand;
import transactionservice.dto.MarkAsCommand;
import transactionservice.entity.TransactionEntity;
import transactionservice.grpc.TransactionStatusStreamRegistry;
import transactionservice.mapper.grpc.TransactionGrpcMapper;
import transactionservice.mapper.result.TransactionResultMapper;
import transactionservice.repository.TransactionRepository;

class TransactionServiceTest {
  private final TransactionRepository transactions = mock(TransactionRepository.class);
  private final TransactionStatusStreamRegistry streams =
      mock(TransactionStatusStreamRegistry.class);
  private final TransactionResultMapper transactionResultMapper =
      mock(TransactionResultMapper.class);
  private final ReservationService reservations = mock(ReservationService.class);
  private final RequestFundsService requestFunds = mock(RequestFundsService.class);
  private final TransactionService service =
      new TransactionService(
          transactions,
          mock(TransactionGrpcMapper.class),
          streams,
          transactionResultMapper,
          mock(TransactionIdempotencyService.class),
          reservations,
          requestFunds);
  private final UUID transactionId = UUID.randomUUID();

  @Test
  void identifiesTerminalStatuses() {
    assertThat(service.isTerminalStatus(TransactionStatus.COMPLETED)).isTrue();
    assertThat(service.isTerminalStatus(TransactionStatus.COMPENSATED)).isTrue();
    assertThat(service.isTerminalStatus(TransactionStatus.FAILED)).isTrue();
    assertThat(service.isTerminalStatus(TransactionStatus.FUNDS_RESERVED)).isFalse();
  }

  @Test
  void emptyAccountListReturnsEmptyTransactions() {
    assertThat(service.getTransactionsByAccountIds(List.of())).isEmpty();
    verifyNoInteractions(transactions);
  }

  @Test
  void statusUpdateSkipsMissingSameAndTerminalTransactions() {
    when(transactions.findByIdToUpdate(transactionId)).thenReturn(Optional.empty());
    service.markAs(new MarkAsCommand(transactionId, TransactionStatus.COMPLETED));
    var entity = new TransactionEntity();
    entity.setId(transactionId);
    entity.setStatus(TransactionStatus.FUNDS_RESERVED);
    when(transactions.findByIdToUpdate(transactionId)).thenReturn(Optional.of(entity));
    service.markAs(new MarkAsCommand(transactionId, TransactionStatus.FUNDS_RESERVED));
    entity.setStatus(TransactionStatus.FAILED);
    service.markAs(new MarkAsCommand(transactionId, TransactionStatus.COMPLETED));
    verify(transactions, never()).save(any());
  }

  @Test
  void statusUpdateSavesNonTerminalChangeAndNotifies() {
    var entity = new TransactionEntity();
    entity.setId(transactionId);
    entity.setStatus(TransactionStatus.FUNDS_RESERVED);
    when(transactions.findByIdToUpdate(transactionId)).thenReturn(Optional.of(entity));
    service.markAs(new MarkAsCommand(transactionId, TransactionStatus.COMPLETED));
    assertThat(entity.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    verify(transactions).save(entity);
    verify(streams).notifyStatusChanged(entity);
  }

  @Test
  void delegatesSuccessfulCreationToReservationAndFundsRequestSteps() {
    UUID source = UUID.randomUUID();
    UUID target = UUID.randomUUID();
    var command =
        new CreateTransactionCommand(
            source,
            target,
            100L,
            Currency.USD,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID());
    var entity = new TransactionEntity();
    entity.setId(transactionId);
    entity.setSourceAccountId(source);
    entity.setTargetAccountId(target);
    entity.setMinorUnits(100L);
    entity.setCurrency(Currency.USD);
    when(transactions.findByIdempotencyKey(command.idempotencyKey())).thenReturn(Optional.empty());
    when(transactions.saveAndFlush(any(TransactionEntity.class))).thenReturn(entity);
    when(transactionResultMapper.toCreateTransactionResult(entity)).thenReturn(null);

    assertThat(service.createTransaction(command)).isNull();

    var order = inOrder(reservations, requestFunds, streams);
    order.verify(reservations).reserve(command, entity);
    order.verify(requestFunds).requestFunds(command, entity);
    order.verify(streams).notifyStatusChanged(entity);
  }
}
