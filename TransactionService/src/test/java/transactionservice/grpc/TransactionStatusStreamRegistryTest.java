package transactionservice.grpc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import account.contract.v1.AccountResponse;
import enums.transaction.TransactionStatus;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import transaction.contract.v1.TransactionStatusResponse;
import transactionservice.client.AccountGrpcClient;
import transactionservice.entity.TransactionEntity;
import transactionservice.mapper.grpc.TransactionGrpcMapper;
import transactionservice.repository.TransactionRepository;

class TransactionStatusStreamRegistryTest {
  private final TransactionRepository repository = mock(TransactionRepository.class);
  private final AccountGrpcClient client = mock(AccountGrpcClient.class);
  private final TransactionGrpcMapper mapper = mock(TransactionGrpcMapper.class);
  private final TransactionStatusStreamRegistry registry =
      new TransactionStatusStreamRegistry(mapper, repository, client);

  @Test
  void eitherOwnerCanSubscribeAndTerminalUpdateUsesBatch() {
    UUID sourceOwner = UUID.randomUUID();
    UUID targetOwner = UUID.randomUUID();
    var transaction = transaction();
    var source = account(transaction.getSourceAccountId(), sourceOwner);
    var target = account(transaction.getTargetAccountId(), targetOwner);
    var ids = List.of(transaction.getSourceAccountId(), transaction.getTargetAccountId());
    when(repository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
    when(client.getAccountByIdsForTransaction(ids))
        .thenReturn(
            Map.of(
                transaction.getTargetAccountId(),
                target,
                transaction.getSourceAccountId(),
                source));
    var response = TransactionStatusResponse.getDefaultInstance();
    when(mapper.toTransactionStatusResponse(transaction, source, target)).thenReturn(response);
    StreamObserver<TransactionStatusResponse> sourceObserver = mock(StreamObserver.class);
    StreamObserver<TransactionStatusResponse> targetObserver = mock(StreamObserver.class);
    registry.subscribe(transaction.getId(), UUID.randomUUID(), sourceOwner, sourceObserver);
    registry.subscribe(transaction.getId(), UUID.randomUUID(), targetOwner, targetObserver);
    transaction.setStatus(TransactionStatus.COMPLETED);
    registry.notifyStatusChanged(transaction);
    verify(sourceObserver, times(2)).onNext(response);
    verify(targetObserver, times(2)).onNext(response);
    verify(sourceObserver).onCompleted();
    verify(targetObserver).onCompleted();
    verify(client, times(3)).getAccountByIdsForTransaction(ids);
    verifyNoMoreInteractions(client);
  }

  @Test
  void foreignCallerCannotSubscribe() {
    var transaction = transaction();
    var ids = List.of(transaction.getSourceAccountId(), transaction.getTargetAccountId());
    when(repository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
    when(client.getAccountByIdsForTransaction(ids))
        .thenReturn(
            Map.of(
                transaction.getSourceAccountId(),
                    account(transaction.getSourceAccountId(), UUID.randomUUID()),
                transaction.getTargetAccountId(),
                    account(transaction.getTargetAccountId(), UUID.randomUUID())));
    StreamObserver<TransactionStatusResponse> observer = mock(StreamObserver.class);
    registry.subscribe(transaction.getId(), UUID.randomUUID(), UUID.randomUUID(), observer);
    verify(observer).onError(any());
    verify(observer, never()).onNext(any());
    verifyNoInteractions(mapper);
  }

  private TransactionEntity transaction() {
    var transaction = new TransactionEntity();
    transaction.setId(UUID.randomUUID());
    transaction.setSourceAccountId(UUID.randomUUID());
    transaction.setTargetAccountId(UUID.randomUUID());
    return transaction;
  }

  private AccountResponse account(UUID id, UUID owner) {
    return AccountResponse.newBuilder()
        .setAccountId(id.toString())
        .setAuthUserId(owner.toString())
        .build();
  }
}
