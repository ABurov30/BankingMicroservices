package apigateway.mapper.grpc;

import account.contract.v1.AccountResponse;
import account.contract.v1.CreateAccountGrpcRequest;
import account.contract.v1.GetAccountByIdGrpcRequest;
import account.contract.v1.UpdateAccountBalanceGrpcRequest;
import apigateway.dto.account.CreateAccountRequestDto;
import apigateway.dto.account.GetAccountByIdRequestDto;
import apigateway.dto.account.GetAccountResponseDto;
import apigateway.dto.account.UpdateAccountBalanceRequestDto;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountGrpcMapper {
  default CreateAccountGrpcRequest toCreateAccountGrpcRequest(
      CreateAccountRequestDto request, UUID authUserId) {
    return CreateAccountGrpcRequest.newBuilder()
        .setOwnerUserId(request.ownerUserId().toString())
        .setAuthUserId(authUserId.toString())
        .setType(request.type().name())
        .setCurrency(request.currency().name())
        .build();
  }

  default GetAccountByIdGrpcRequest toGetAccountByIdGrpcRequest(GetAccountByIdRequestDto request) {
    return GetAccountByIdGrpcRequest.newBuilder()
        .setAccountId(request.accountId().toString())
        .build();
  }

  default UpdateAccountBalanceGrpcRequest toUpdateAccountBalanceGrpcRequest(
      UpdateAccountBalanceRequestDto request, UUID authUserId) {
    return UpdateAccountBalanceGrpcRequest.newBuilder()
        .setAccountId(request.accountId().toString())
        .setMinorUnits(request.minorUnits().longValue())
        .setAuthUserId(authUserId.toString())
        .build();
  }

  default AccountResponse toAccountResponse(GetAccountResponseDto response) {
    return AccountResponse.newBuilder()
        .setAccountNumber(response.accountNumber())
        .setType(response.type().name())
        .setAccountId(response.accountId().toString())
        .setAvailableBalance(response.availableBalance().longValue())
        .setReservedBalance(response.reservedBalance().longValue())
        .setOwnerUserId(response.ownerUserId().toString())
        .setStatus(response.status().name())
        .setCurrency(response.currency().name())
        .build();
  }
}
