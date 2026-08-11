package transactionservice.mapper.dto;

import account.contract.v1.AccountResponse;
import account.contract.v1.ReserveFundsForTransactionGrpcResponse;
import enums.account.AccountCurrency;
import enums.account.AccountStatus;
import enums.account.AccountType;
import enums.account.ReservationStatus;
import org.mapstruct.Mapper;
import transactionservice.dto.AccountResponseDto;
import transactionservice.dto.ReserveFudsForTransactionResponseDto;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface TransactionDtoMapper {

    default AccountResponseDto toAccountResponseDto(AccountResponse response) {
        if (response == null || response.getAccountId().isBlank()) {
            return null;
        }

        return new AccountResponseDto(
                UUID.fromString(response.getAccountId()),
                UUID.fromString(response.getOwnerUserId()),
                response.getAccountNumber(),
                AccountType.valueOf(response.getType()),
                AccountStatus.valueOf(response.getStatus()),
                BigDecimal.valueOf(response.getAvailableBalance()),
                BigDecimal.valueOf(response.getReservedBalance()),
                AccountCurrency.valueOf(response.getCurrency())
        );
    }

    default ReserveFudsForTransactionResponseDto toReserveFudsForTransactionResponseDto(ReserveFundsForTransactionGrpcResponse response) {
        return new ReserveFudsForTransactionResponseDto(
                response.hasSourceAccount()
                        ? toAccountResponseDto(response.getSourceAccount())
                        : null,
                response.hasTargetAccount()
                        ? toAccountResponseDto(response.getTargetAccount())
                        : null,
                ReservationStatus.valueOf(response.getStatus()),
                response.getMessage()
        );
    }
}
