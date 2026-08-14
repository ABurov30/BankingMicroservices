package apigateway.mapper.grpc;

import account.contract.v1.AccountResponse;
import apigateway.dto.account.GetAccountResponseDto;
import apigateway.dto.transaction.CreateTransactionRequestDto;
import apigateway.dto.transaction.TransactionResponseDto;
import enums.account.AccountCurrency;
import enums.account.AccountStatus;
import enums.account.AccountType;
import enums.transaction.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import transaction.contract.v1.CreateTransactionGrpcRequest;
import transaction.contract.v1.GetTransactionsByAccountsGrpcRequest;
import transaction.contract.v1.GetTransactionsByAccountsGrpcResponse;
import transaction.contract.v1.TransactionResponse;

@Mapper(componentModel = "spring")
public interface TransactionGrpcMapper {

  default CreateTransactionGrpcRequest toCreateTransactionGrpcRequest(
      CreateTransactionRequestDto request, UUID sourceAuthUserId, UUID targetAuthUserId) {
    return CreateTransactionGrpcRequest.newBuilder()
        .setSourceAccountId(request.sourceAccountId().toString())
        .setTargetAccountId(request.targetAccountId().toString())
        .setAmount(request.amount().longValue())
        .setCurrency(request.currency().name())
        .setIdempotencyKey(request.idempotencyKey().toString())
        .setSourceAuthUserId(sourceAuthUserId.toString())
        .setTargetAuthUserId(targetAuthUserId.toString())
        .setSourceCardId(request.sourceCardId().toString())
        .build();
  }

  default GetTransactionsByAccountsGrpcRequest toGetTransactionsByAccountsGrpcRequest(
      List<AccountResponse> accountResponseList) {
    return GetTransactionsByAccountsGrpcRequest.newBuilder()
        .addAllAccounts(accountResponseList)
        .build();
  }

  default List<TransactionResponseDto> toTransactionResponseDtos(
      GetTransactionsByAccountsGrpcResponse response) {
    return response.getTransactionsList().stream().map(this::toTransactionResponseDto).toList();
  }

  default TransactionResponseDto toTransactionResponseDto(TransactionResponse response) {
    return new TransactionResponseDto(
        BigDecimal.valueOf(response.getAmount()),
        AccountCurrency.valueOf(response.getCurrency()),
        TransactionStatus.valueOf(response.getStatus()),
        toLocalDateTime(response.getCreatedAt()),
        toLocalDateTime(response.getCompletedAt()),
        response.hasSourceAccount() ? toGetAccountResponseDto(response.getSourceAccount()) : null,
        response.hasTargetAccount() ? toGetAccountResponseDto(response.getTargetAccount()) : null);
  }

  private GetAccountResponseDto toGetAccountResponseDto(AccountResponse account) {
    return new GetAccountResponseDto(
        UUID.fromString(account.getAccountId()),
        UUID.fromString(account.getOwnerUserId()),
        account.getAccountNumber(),
        AccountType.valueOf(account.getType()),
        AccountStatus.valueOf(account.getStatus()),
        BigDecimal.valueOf(account.getAvailableBalance()),
        BigDecimal.valueOf(account.getReservedBalance()),
        AccountCurrency.valueOf(account.getCurrency()));
  }

  private LocalDateTime toLocalDateTime(String value) {
    return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
  }
}
