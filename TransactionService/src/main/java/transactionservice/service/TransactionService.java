package transactionservice.service;

import enums.account.ReservationStatus;
import enums.transaction.TransactionStatus;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kafkacontracts.transaction.TransactionEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import transactionservice.client.AccountGrpcClient;
import transactionservice.client.CardGrpcClient;
import transactionservice.dto.CreateTransactionCommand;
import transactionservice.dto.MarkAsCommand;
import transactionservice.dto.ReservationResponseDto;
import transactionservice.entity.TransactionEntity;
import transactionservice.entity.TransactionOutboxEventEntity;
import transactionservice.exception.FundsReservationFailedException;
import transactionservice.grpc.TransactionStatusStreamRegistry;
import transactionservice.mapper.grpc.TransactionGrpcMapper;
import transactionservice.repository.TransactionOutboxEventRepository;
import transactionservice.repository.TransactionRepository;

@Service
public class TransactionService {
  private final AccountGrpcClient accountGrpcClient;
  private final TransactionOutboxEventRepository transactionOutboxEventRepository;
  private final TransactionRepository transactionRepository;
  private final TransactionGrpcMapper grpcMapper;
  private final CardGrpcClient cardGrpcClient;
  private final TransactionStatusStreamRegistry transactionStatusStreamRegistry;
  private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

  public TransactionService(
      AccountGrpcClient accountGrpcClient,
      TransactionOutboxEventRepository transactionOutboxEventRepository,
      TransactionRepository transactionRepository,
      TransactionGrpcMapper grpcMapper,
      CardGrpcClient cardGrpcClient,
      TransactionStatusStreamRegistry transactionStatusStreamRegistry) {
    this.accountGrpcClient = accountGrpcClient;
    this.transactionRepository = transactionRepository;
    this.transactionOutboxEventRepository = transactionOutboxEventRepository;
    this.grpcMapper = grpcMapper;
    this.cardGrpcClient = cardGrpcClient;
    this.transactionStatusStreamRegistry = transactionStatusStreamRegistry;
  }

  private TransactionEntity saveTransaction(CreateTransactionCommand command) {
    var transaction = new TransactionEntity();
    transaction.setSourceAccountId(command.sourceAccountId());
    transaction.setTargetAccountId(command.targetAccountId());
    transaction.setIdempotencyKey(command.idempotencyKey());
    transaction.setAmount(command.amount());
    transaction.setCurrency(command.currency());
    transaction.setStatus(TransactionStatus.FUNDS_RESERVED);
    return transactionRepository.save(transaction);
  }

  private TransactionOutboxEventEntity saveTransactionOutboxEvent(
      TransactionEntity transaction, TransactionEventType eventType, Map<String, Object> payload) {
    var transactionOutboxEvent = new TransactionOutboxEventEntity();
    transactionOutboxEvent.setAggregateType("TRANSACTION_TYPE");
    transactionOutboxEvent.setAggregateId(transaction.getId());
    transactionOutboxEvent.setEventType(eventType.name());
    transactionOutboxEvent.setTopic(eventType.getTopic());
    transactionOutboxEvent.setEventKey(transaction.getId() + ":" + eventType.name());
    transactionOutboxEvent.setSchemaVersion(eventType.getVersion());
    transactionOutboxEvent.setPayload(payload);
    return transactionOutboxEventRepository.save(transactionOutboxEvent);
  }

  private void onReservationFailed(
      TransactionEntity transaction,
      ReservationResponseDto reservationResponse,
      CreateTransactionCommand command,
      String exceptionMessage) {
    transaction.setErrorMessage(reservationResponse.message());
    transaction.setStatus(TransactionStatus.FAILED);
    transaction.setErrorMessage(reservationResponse.message());
    transactionRepository.save(transaction);
    transactionStatusStreamRegistry.notifyStatusChanged(transaction);
    saveTransactionOutboxEvent(
        transaction,
        TransactionEventType.TRANSACTION_FAILED,
        Map.of(
            "amount", transaction.getAmount(),
            "authUserId", command.sourceAuthUserId()));
    throw new FundsReservationFailedException(exceptionMessage + " " + transaction.getId());
  }

  @Transactional
  public void createTransaction(CreateTransactionCommand command) {
    var transaction = saveTransaction(command);
    var reservationLimitsResponse =
        cardGrpcClient.reserveLimitsForTransaction(
            grpcMapper.toReserveLimitsForTransactionGrpcRequest(transaction, command));

    if (reservationLimitsResponse.status() == ReservationStatus.FAILED) {
      onReservationFailed(
          transaction, reservationLimitsResponse, command, "Reservation limits failed");
    }

    var reservationFundsResponse =
        accountGrpcClient.reserveFundsForTransaction(
            grpcMapper.toReserveFundsForTransactionGrpcRequest(
                transaction, command.sourceAuthUserId()));

    if (reservationFundsResponse.reservationResponse().status() == ReservationStatus.FAILED) {
      onReservationFailed(
          transaction,
          reservationFundsResponse.reservationResponse(),
          command,
          "Reservation funds failed");
    }

    transaction.setStatus(TransactionStatus.FUNDS_REQUESTED);
    transactionRepository.save(transaction);
    transactionStatusStreamRegistry.notifyStatusChanged(transaction);
    saveTransactionOutboxEvent(
        transaction,
        TransactionEventType.TRANSACTION_FUNDS_REQUESTED,
        Map.of(
            "transactionId", transaction.getId(),
            "targetAccountId", transaction.getTargetAccountId(),
            "authUserId", command.targetAuthUserId()));
  }

  public List<TransactionEntity> getTransactionsByAccountIds(Collection<UUID> accountIds) {
    if (accountIds.isEmpty()) {
      return List.of();
    }

    return transactionRepository.findByAccountIds(accountIds);
  }

  @Transactional
  public void markAs(MarkAsCommand command) {
    var transaction = transactionRepository.findByIdToUpdate(command.transactionId());

    if (transaction.isEmpty()) {
      log.warn(
          "Skipping transaction status update: transactionId={} not found",
          command.transactionId());
      return;
    }

    var transactionEntity = transaction.get();
    if (transactionEntity.getStatus() == command.status()) {
      log.info(
          "Skipping transaction status update: transactionId={}, status={}",
          command.transactionId(),
          transactionEntity.getStatus());
      return;
    }

    if (isTerminalStatus(transactionEntity.getStatus())) {
      log.info(
          "Skipping transaction status update: "
              + "transactionId={}, currentStatus={}, requestedStatus={}",
          command.transactionId(),
          transactionEntity.getStatus(),
          command.status());
      return;
    }

    transactionEntity.setCompletedAdt(LocalDateTime.now());
    transactionEntity.setStatus(command.status());
    transactionRepository.save(transactionEntity);
    transactionStatusStreamRegistry.notifyStatusChanged(transactionEntity);
  }

  public boolean isTerminalStatus(TransactionStatus status) {
    return status == TransactionStatus.COMPLETED
        || status == TransactionStatus.COMPENSATED
        || status == TransactionStatus.FAILED;
  }
}
