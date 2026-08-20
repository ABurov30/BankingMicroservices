package transactionservice.grpc;

import account.contract.v1.AccountResponse;
import enums.transaction.TransactionStatus;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import transaction.contract.v1.TransactionStatusResponse;
import transactionservice.client.AccountGrpcClient;
import transactionservice.entity.TransactionEntity;
import transactionservice.mapper.grpc.TransactionGrpcMapper;
import transactionservice.repository.TransactionRepository;

@Service
public class TransactionStatusStreamRegistry {
  private final Map<UUID, Map<UUID, StreamObserver<TransactionStatusResponse>>> subscribers =
      new ConcurrentHashMap<>();

  private final TransactionGrpcMapper transactionGrpcMapper;
  private final TransactionRepository transactionRepository;
  private final AccountGrpcClient accountGrpcClient;
  private static final Logger log = LoggerFactory.getLogger(TransactionStatusStreamRegistry.class);

  public TransactionStatusStreamRegistry(
      TransactionGrpcMapper grpcMapper,
      TransactionRepository transactionRepository,
      AccountGrpcClient accountGrpcClient) {
    this.transactionGrpcMapper = grpcMapper;
    this.transactionRepository = transactionRepository;
    this.accountGrpcClient = accountGrpcClient;
  }

  public void subscribe(
      UUID transactionId,
      UUID subscriptionKey,
      UUID authUserId,
      StreamObserver<TransactionStatusResponse> observer) {

    var allowedTransaction = findAllowedTransaction(transactionId, authUserId);
    if (allowedTransaction == null) {
      observer.onError(
          Status.PERMISSION_DENIED
              .withDescription("User is not allowed to watch this transaction")
              .asRuntimeException());
      return;
    }

    subscribers
        .computeIfAbsent(transactionId, ignored -> new ConcurrentHashMap<>())
        .put(subscriptionKey, observer);

    TransactionStatusResponse response =
        transactionGrpcMapper.toTransactionStatusResponse(
            allowedTransaction.transaction(),
            allowedTransaction.sourceAccount(),
            allowedTransaction.targetAccount());
    observer.onNext(response);

    if (isTerminalStatus(allowedTransaction.transaction().getStatus())) {
      unsubscribe(transactionId, subscriptionKey);
      observer.onCompleted();
    }
  }

  public void unsubscribe(UUID transactionId, UUID subscriptionKey) {
    var transactionSubscribers = subscribers.get(transactionId);

    if (transactionSubscribers == null) {
      return;
    }

    transactionSubscribers.remove(subscriptionKey);

    if (transactionSubscribers.isEmpty()) {
      subscribers.remove(transactionId);
    }
  }

  public void notifyStatusChanged(TransactionEntity transaction) {
    var sourceAccount = accountGrpcClient.getAccountById(transaction.getSourceAccountId());
    var targetAccount = accountGrpcClient.getAccountById(transaction.getTargetAccountId());
    TransactionStatusResponse response =
        transactionGrpcMapper.toTransactionStatusResponse(
            transaction, sourceAccount, targetAccount);

    subscribers
        .getOrDefault(transaction.getId(), Map.of())
        .forEach((ignored, observer) -> observer.onNext(response));

    if (isTerminalStatus(transaction.getStatus())) {
      var observers = subscribers.get(transaction.getId());
      if (observers != null) {
        observers.forEach((ignored, observer) -> observer.onCompleted());
        subscribers.remove(transaction.getId());
      }
    }
  }

  private boolean isTerminalStatus(TransactionStatus status) {
    return status == TransactionStatus.COMPLETED
        || status == TransactionStatus.COMPENSATED
        || status == TransactionStatus.FAILED;
  }

  private AllowedTransaction findAllowedTransaction(UUID transactionId, UUID authUserId) {
    var transaction = transactionRepository.findById(transactionId);

    if (transaction.isEmpty()) {
      log.warn("Transaction not found: transactionId={}", transactionId);
      return null;
    }

    var transactionEntity = transaction.get();

    var sourceAccount = accountGrpcClient.getAccountById(transactionEntity.getSourceAccountId());
    var targetAccount = accountGrpcClient.getAccountById(transactionEntity.getTargetAccountId());

    if (sourceAccount != null && sourceAccount.getAuthUserId().equals(authUserId.toString())) {
      return new AllowedTransaction(transactionEntity, sourceAccount, targetAccount);
    }

    if (targetAccount != null && targetAccount.getAuthUserId().equals(authUserId.toString())) {
      return new AllowedTransaction(transactionEntity, sourceAccount, targetAccount);
    }

    log.warn(
        "User is not allowed to watch transaction: transactionId={}, authUserId={}",
        transactionId,
        authUserId);

    return null;
  }

  private record AllowedTransaction(
      TransactionEntity transaction,
      AccountResponse sourceAccount,
      AccountResponse targetAccount) {}
}
