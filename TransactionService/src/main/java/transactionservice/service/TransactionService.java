package transactionservice.service;

import enums.account.ReservationStatus;
import enums.transaction.TransactionStatus;
import jakarta.transaction.Transactional;
import kafkacontracts.transaction.TransactionEventType;
import org.springframework.stereotype.Service;
import transactionservice.client.AccountGrpcClient;
import transactionservice.dto.CreateTransactionCommand;
import transactionservice.dto.MarkAsCommand;
import transactionservice.exception.FundsReservationFailedException;
import transactionservice.exception.TransactionNotFoundException;
import transactionservice.entity.TransactionEntity;
import transactionservice.entity.TransactionOutboxEventEntity;
import transactionservice.mapper.grpc.TransactionGrpcMapper;
import transactionservice.repository.TransactionOutboxEventRepository;
import transactionservice.repository.TransactionRepository;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class TransactionService {
    private final AccountGrpcClient accountGrpcClient;
    private final TransactionOutboxEventRepository transactionOutboxEventRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionGrpcMapper grpcMapper;


    public TransactionService(
            AccountGrpcClient accountGrpcClient,
            TransactionOutboxEventRepository transactionOutboxEventRepository,
            TransactionRepository transactionRepository,
            TransactionGrpcMapper grpcMapper
    ) {
        this.accountGrpcClient = accountGrpcClient;
        this.transactionRepository = transactionRepository;
        this.transactionOutboxEventRepository = transactionOutboxEventRepository;
        this.grpcMapper = grpcMapper;
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

    private TransactionOutboxEventEntity saveTransactionOutboxEvent(TransactionEntity transaction, TransactionEventType eventType, Map<String, Object> payload) {
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


    @Transactional
    public void createTransaction(CreateTransactionCommand command) {
        var transaction = saveTransaction(command);
        var reservationResponse = accountGrpcClient.reserveFundsForTransaction(grpcMapper.toReserveFundsForTransactionGrpcRequest(transaction));

        if (reservationResponse.status() == ReservationStatus.FAILED) {
            transaction.setErrorMessage(reservationResponse.message());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setErrorMessage(reservationResponse.message());
            transactionRepository.save(transaction);
            saveTransactionOutboxEvent(
                    transaction,
                    TransactionEventType.TRANSACTION_FAILED,
                    Map.of(
                            "accountNumber", reservationResponse.targetAccount().accountNumber(),
                            "amount", transaction.getAmount(),
                            "authUserId", command.sourceAuthUserId()
                    )
            );
            throw new FundsReservationFailedException("Funds reservation failed");
        }


        transaction.setStatus(TransactionStatus.FUNDS_REQUESTED);
        transactionRepository.save(transaction);
        saveTransactionOutboxEvent(
                transaction,
                TransactionEventType.TRANSACTION_FUNDS_REQUESTED,
                Map.of(
                        "transactionId", transaction.getId(),
                        "targetAccountId", transaction.getTargetAccountId(),
                        "authUserId", command.targetAuthUserId()
                )
        );
    }

    @Transactional
    public void  markAs(MarkAsCommand command) {
        var transaction = transactionRepository.findByIdToUpdate(command.transactionId())
                .orElseThrow(() -> new TransactionNotFoundException(command.transactionId()));
        if (transaction.getStatus() == command.status()) {
            return;
        }
        transaction.setCompletedAdt(LocalDateTime.now());
        transaction.setStatus(command.status());
        transactionRepository.save(transaction);
    }
}
