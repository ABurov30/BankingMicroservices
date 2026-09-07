package apigateway.mapper.grpc;

import account.contract.v1.*;
import apigateway.dto.command.account.GetAccountsWithCardsByOwnerIdCommandDto;
import apigateway.dto.request.account.CreateAccountRequestDto;
import apigateway.dto.request.account.GetAccountByIdRequestDto;
import apigateway.dto.request.account.GetAllAccountsRequestDto;
import apigateway.dto.request.account.UpdateAccountBalanceRequestDto;
import apigateway.dto.response.account.GetAccountResponseDto;
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
        .setAuthUserId(request.authUserId().toString())
        .setRole(request.role().name())
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
        .setAvailableBalanceMinorUnits(response.availableBalanceMinorUnits())
        .setReservedBalanceMinorUnits(response.reservedBalanceMinorUnits())
        .setOwnerUserId(response.ownerUserId().toString())
        .setStatus(response.status().name())
        .setCurrency(response.currency().name())
        .build();
  }

  default GetAccountByOwnerUserIdGrpcRequest toGetAccountByOwnerUserIdGrpcRequest(
      GetAccountsWithCardsByOwnerIdCommandDto command) {
    return GetAccountByOwnerUserIdGrpcRequest.newBuilder()
        .setOwnerUserId(command.ownerUserId().toString())
        .setAuthUserId(command.authUserId().toString())
        .setRole(command.role().name())
        .build();
  }

  default GetAccountByOwnerUserIdGrpcRequest toGetAccountByOwnerUserIdGrpcRequest(
      UUID ownerUserId) {
    return GetAccountByOwnerUserIdGrpcRequest.newBuilder()
        .setOwnerUserId(ownerUserId.toString())
        .build();
  }

  default FreezeAccountGrpcRequest toFreezeAccountGrpcRequest(
      UUID accountId, UUID authUserId, String role) {
    return FreezeAccountGrpcRequest.newBuilder()
        .setAccountId(accountId.toString())
        .setAuthUserId(authUserId.toString())
        .setRole(role == null ? "" : role)
        .build();
  }

  default UnfreezeAccountGrpcRequest toUnfreezeAccountGrpcRequest(
      UUID accountId, UUID authUserId, String role) {
    return UnfreezeAccountGrpcRequest.newBuilder()
        .setAccountId(accountId.toString())
        .setAuthUserId(authUserId.toString())
        .setRole(role == null ? "" : role)
        .build();
  }

  default GetAllAccountsGrpRequest toGetAllAccountsGrpRequest(GetAllAccountsRequestDto request) {
    return GetAllAccountsGrpRequest.newBuilder().setRole(request.role().name()).build();
  }

  default GetRecipientAccountsByOwnerUserIdGrpcRequest
      toGetRecipientAccountsByOwnerUserIdGrpcRequest(UUID ownerUserId) {
    return GetRecipientAccountsByOwnerUserIdGrpcRequest.newBuilder()
        .setOwnerUserId(ownerUserId.toString())
        .build();
  }
}
