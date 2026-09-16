package transactionservice.service;

import enums.transaction.TransactionStatus;
import java.time.LocalDateTime;
import java.util.Map;
import kafkacontracts.transaction.TransactionEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import transactionservice.dto.CreateTransactionCommand;
import transactionservice.dto.ReservationResponseDto;
import transactionservice.entity.TransactionEntity;
import transactionservice.repository.TransactionRepository;

@Service
@RequiredArgsConstructor
public class ReservationFailureService {
  private final TransactionRepository transactionRepository;
  private final TransactionOutboxService transactionOutboxService;

  private void createCompensationOutbox(TransactionEntity transaction) {

    transactionOutboxService.saveTransactionOutboxEvent(
        transaction,
        TransactionEventType.TRANSACTION_CARD_LIMIT_HOLD_COMPENSATION,
        Map.of("transactionId", transaction.getId()));
  }

  @Transactional
  public void onReservationFailed(
      TransactionEntity transaction,
      ReservationResponseDto reservationResponse,
      CreateTransactionCommand command) {
    createCompensationOutbox(transaction);

    transaction.setErrorMessage(reservationResponse.message());
    transaction.setStatus(TransactionStatus.FAILED);
    transaction.setCompletedAt(LocalDateTime.now());
    transactionRepository.save(transaction);

    transactionOutboxService.saveTransactionOutboxEvent(
        transaction,
        TransactionEventType.TRANSACTION_FAILED,
        Map.of(
            "amountMinorUnits",
            transaction.getMinorUnits(),
            "currency",
            transaction.getCurrency().name(),
            "authUserId",
            command.sourceAuthUserId()));
  }
}
