package transactionservice.service;

import enums.account.ReservationStatus;
import enums.transaction.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import transactionservice.client.AccountGrpcClient;
import transactionservice.client.CardGrpcClient;
import transactionservice.dto.CreateTransactionCommand;
import transactionservice.dto.ReservationResponseDto;
import transactionservice.entity.TransactionEntity;
import transactionservice.exception.FundsReservationFailedException;
import transactionservice.grpc.TransactionStatusStreamRegistry;
import transactionservice.mapper.grpc.TransactionGrpcMapper;
import transactionservice.repository.TransactionRepository;

@Service
@RequiredArgsConstructor
public class ReservationService {
  private final AccountGrpcClient accountGrpcClient;
  private final TransactionGrpcMapper grpcMapper;
  private final CardGrpcClient cardGrpcClient;
  private final ReservationFailureService reservationFailureService;
  private final TransactionRepository transactionRepository;
  private final TransactionStatusStreamRegistry transactionStatusStreamRegistry;

  public void reserve(CreateTransactionCommand command, TransactionEntity transaction) {
    try {
      reserveCardLimits(command, transaction);

      reserveFunds(command, transaction);
    } catch (Exception e) {
      reservationFailureService.onReservationFailed(
          transaction,
          new ReservationResponseDto(ReservationStatus.FAILED, e.getMessage()),
          command);

      transactionStatusStreamRegistry.notifyStatusChanged(transaction);

      throw new FundsReservationFailedException(e.getMessage() + " " + transaction.getId());
    }
  }

  private void reserveCardLimits(CreateTransactionCommand command, TransactionEntity transaction) {
    var reservationLimitsResponse =
        cardGrpcClient.reserveLimitsForTransaction(
            grpcMapper.toReserveLimitsForTransactionGrpcRequest(transaction, command));

    if (reservationLimitsResponse.status() == ReservationStatus.FAILED) {
      throw new RuntimeException(
          "Unsuccess to create card limit hold reservation" + reservationLimitsResponse.message());
    }

    markCardReserved(transaction, TransactionStatus.CARD_LIMIT_RESERVED);
  }

  private TransactionEntity markCardReserved(
      TransactionEntity transaction, TransactionStatus status) {
    transaction.setStatus(status);
    return transactionRepository.saveAndFlush(transaction);
  }

  private void reserveFunds(CreateTransactionCommand command, TransactionEntity transaction) {
    var reservationFundsResponse =
        accountGrpcClient.reserveFundsForTransaction(
            grpcMapper.toReserveFundsForTransactionGrpcRequest(
                transaction, command.sourceAuthUserId()));

    if (reservationFundsResponse.reservationResponse().status() == ReservationStatus.FAILED) {
      throw new RuntimeException(
          "Unsuccess to create account hold reservation "
              + reservationFundsResponse.reservationResponse().message());
    }

    markCardReserved(transaction, TransactionStatus.FUNDS_RESERVED);
  }
}
