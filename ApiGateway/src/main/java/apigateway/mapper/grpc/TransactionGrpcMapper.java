package apigateway.mapper.grpc;

import account.contract.v1.AccountResponse;
import account.contract.v1.AccountResponseWithoutSensitiveInfo;
import apigateway.dto.account.GetAccountResponseDto;
import apigateway.dto.transaction.CreateTransactionRequestDto;
import apigateway.dto.transaction.TransactionResponseDto;
import apigateway.dto.transaction.TransactionStatusAccountResponseDto;
import apigateway.dto.transaction.TransactionStatusResponseDto;
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
import transaction.contract.v1.TransactionStatusResponse;
import transaction.contract.v1.WatchTransactionStatusRequest;

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

  default WatchTransactionStatusRequest toWatchTransactionStatusRequest(
      UUID transactionId, UUID authUserId, UUID subscriptionKey) {
    return WatchTransactionStatusRequest.newBuilder()
        .setTransactionId(transactionId.toString())
        .setAuthUserId(authUserId.toString())
        .setSubscriptionKey(subscriptionKey.toString())
        .build();
  }

  default List<TransactionResponseDto> toTransactionResponseDtos(
      GetTransactionsByAccountsGrpcResponse response) {
    return response.getTransactionsList().stream().map(this::toTransactionResponseDto).toList();
  }

  default TransactionResponseDto toTransactionResponseDto(TransactionResponse response) {
    return new TransactionResponseDto(
        UUID.fromString(response.getTransactionId()),
        BigDecimal.valueOf(response.getAmount()),
        AccountCurrency.valueOf(response.getCurrency()),
        TransactionStatus.valueOf(response.getStatus()),
        toLocalDateTime(response.getCreatedAt()),
        toLocalDateTime(response.getCompletedAt()),
        response.hasSourceAccount() ? toGetAccountResponseDto(response.getSourceAccount()) : null,
        response.hasTargetAccount() ? toGetAccountResponseDto(response.getTargetAccount()) : null);
  }

  default TransactionStatusResponseDto toTransactionStatusResponseDto(
      TransactionStatusResponse response) {
    return new TransactionStatusResponseDto(
        BigDecimal.valueOf(response.getAmount()),
        AccountCurrency.valueOf(response.getCurrency()),
        TransactionStatus.valueOf(response.getStatus()),
        response.hasSourceAccount()
            ? toTransactionStatusAccountResponseDto(response.getSourceAccount())
            : null,
        response.hasTargetAccount()
            ? toTransactionStatusAccountResponseDto(response.getTargetAccount())
            : null);
  }

  private TransactionStatusAccountResponseDto toTransactionStatusAccountResponseDto(
      AccountResponseWithoutSensitiveInfo account) {
    return new TransactionStatusAccountResponseDto(
        account.getAccountNumber(), AccountCurrency.valueOf(account.getCurrency()));
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
