package accountservice.mapper.command;

import account.contract.v1.ReserveFundsForTransactionGrpcRequest;
import accountservice.dto.CompensationFundsCommand;
import accountservice.dto.ExecuteFundsTransferCommand;
import accountservice.dto.ReserveFundsForTransactionCommand;
import accountservice.dto.TransactionFundsRequestCommand;
import accountservice.entity.AccountHoldEntity;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransferCommandMapper {
  default CompensationFundsCommand toCompensationFundsCommand(AccountHoldEntity accountHold) {
    return new CompensationFundsCommand(accountHold.getId());
  }

  default ExecuteFundsTransferCommand toExecuteFundsTransferCommand(
      TransactionFundsRequestCommand command, AccountHoldEntity accountHold) {
    return new ExecuteFundsTransferCommand(
        command.transactionId(),
        command.targetAccountId(),
        command.targetAuthUserId(),
        accountHold);
  }

  default ReserveFundsForTransactionCommand toReserveFundsForTransactionCommand(
      ReserveFundsForTransactionGrpcRequest request) {
    return new ReserveFundsForTransactionCommand(
        UUID.fromString(request.getSourceAccountId()),
        UUID.fromString(request.getTargetAccountId()),
        request.getMinorUnits(),
        UUID.fromString(request.getTransactionId()),
        UUID.fromString(request.getSourceAuthUserId()));
  }
}
