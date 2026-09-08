package transactionservice.service;

import account.contract.v1.AccountResponse;
import account.contract.v1.RecipientAccount;
import enums.account.ReservationStatus;
import enums.transaction.TransactionStatus;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import kafkacontracts.transaction.TransactionEventType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import transaction.contract.v1.TransactionResponse;
import transactionservice.client.AccountGrpcClient;
import transactionservice.client.CardGrpcClient;
import transactionservice.dto.CreateTransactionCommand;
import transactionservice.dto.CreateTransactionResult;
import transactionservice.dto.MarkAsCommand;
import transactionservice.dto.ReservationResponseDto;
import transactionservice.entity.TransactionEntity;
import transactionservice.entity.TransactionOutboxEventEntity;
import transactionservice.exception.FundsReservationFailedException;
import transactionservice.exception.IdempotencyPayloadMismatchException;
import transactionservice.grpc.TransactionStatusStreamRegistry;
import transactionservice.mapper.grpc.TransactionGrpcMapper;
import transactionservice.mapper.result.TransactionResultMapper;
import transactionservice.repository.TransactionOutboxEventRepository;
import transactionservice.repository.TransactionRepository;

@Service
@RequiredArgsConstructor
public class TransactionService {
  private final AccountGrpcClient accountGrpcClient;
  private final TransactionOutboxEventRepository transactionOutboxEventRepository;
  private final TransactionRepository transactionRepository;
  private final TransactionGrpcMapper grpcMapper;
  private final CardGrpcClient cardGrpcClient;
  private final TransactionStatusStreamRegistry transactionStatusStreamRegistry;
  private final TransactionResultMapper transactionResultMapper;
  private final TransactionIdempotencyService transactionIdempotencyService;
  private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

  private TransactionEntity saveTransaction(CreateTransactionCommand command) {
    var transaction = new TransactionEntity();
    transaction.setSourceAccountId(command.sourceAccountId());
    transaction.setTargetAccountId(command.targetAccountId());
    transaction.setIdempotencyKey(command.idempotencyKey());
    transaction.setMinorUnits(command.minorUnits());
    transaction.setCurrency(command.currency());
    transaction.setStatus(TransactionStatus.FUNDS_RESERVED);
    return transactionRepository.saveAndFlush(transaction);
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
    transaction.setCompletedAt(LocalDateTime.now());
    transactionRepository.save(transaction);
    transactionStatusStreamRegistry.notifyStatusChanged(transaction);

    saveTransactionOutboxEvent(
        transaction,
        TransactionEventType.TRANSACTION_FAILED,
        Map.of(
            "amountMinorUnits",
            transaction.getMinorUnits(),
            "currency",
            transaction.getCurrency().name(),
            "authUserId",
            command.sourceAuthUserId()));
    throw new FundsReservationFailedException(exceptionMessage + " " + transaction.getId());
  }

  @Transactional(dontRollbackOn = FundsReservationFailedException.class)
  public CreateTransactionResult createTransaction(CreateTransactionCommand command) {

    var optionalTransaction = transactionRepository.findByIdempotencyKey(command.idempotencyKey());

    if (optionalTransaction.isPresent()) {

      var existedTransaction = optionalTransaction.get();

      if (!transactionIdempotencyService.checkIsSamePayloadOfTransaction(
          command, existedTransaction)) {
        throw new IdempotencyPayloadMismatchException(command.idempotencyKey());
      }

      return transactionResultMapper.toCreateTransactionResult(existedTransaction);
    }

    TransactionEntity transaction;
    try {
      transaction = saveTransaction(command);
    } catch (DataIntegrityViolationException ex) {
      return transactionIdempotencyService.getExistingTransactionResult(command);
    }

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
            "authUserId", command.sourceAuthUserId()));

    return transactionResultMapper.toCreateTransactionResult(transaction);
  }

  public List<TransactionResponse> getTransactionsByAccountIds(List<AccountResponse> accountList) {

    var accountIds =
        accountList.stream().map((account) -> UUID.fromString(account.getAccountId())).toList();

    if (accountIds.isEmpty()) {
      return List.of();
    }

    ConcurrentMap<UUID, RecipientAccount> accountMap =
        accountList.stream()
            .collect(
                Collectors.toConcurrentMap(
                    account -> UUID.fromString(account.getAccountId()),
                    account -> grpcMapper.toRecipientAccount(account)));

    var transactions = transactionRepository.findByAccountIds(accountIds);

    return transactions.stream()
        .map(
            (transaction -> {
              var targetAccount = accountMap.get(transaction.getTargetAccountId());
              var sourceAccount = accountMap.get(transaction.getSourceAccountId());

              return grpcMapper.toTransactionResponse(transaction, sourceAccount, targetAccount);
            }))
        .toList();
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

    transactionEntity.setCompletedAt(LocalDateTime.now());
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
