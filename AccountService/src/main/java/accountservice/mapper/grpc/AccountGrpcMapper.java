package accountservice.mapper.grpc;

import account.contract.v1.*;
import accountservice.dto.CreateAccountResult;
import accountservice.dto.GetAccountResult;
import accountservice.dto.ReserveFundsForTransactionResult;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountGrpcMapper {
  default AccountResponse toAccountResponse(GetAccountResult value) {
    return AccountResponse.newBuilder()
        .setAccountId(value.accountId().toString())
        .setOwnerUserId(value.ownerUserId().toString())
        .setAuthUserId(value.authUserId().toString())
        .setAccountNumber(value.accountNumber())
        .setType(value.type().name())
        .setStatus(value.status().name())
        .setAvailableBalance(
            value.availableBalance().movePointRight(value.currency().getMinorUnit()).longValue())
        .setReservedBalance(
            value.reservedBalance().movePointRight(value.currency().getMinorUnit()).longValue())
        .setCurrency(value.currency().name())
        .build();
  }

  default AccountResponse toAccountResponse(CreateAccountResult value) {
    return AccountResponse.newBuilder()
        .setAccountId(value.accountId().toString())
        .setOwnerUserId(value.ownerUserId().toString())
        .setAuthUserId(value.authUserId().toString())
        .setAccountNumber(value.accountNumber())
        .setType(value.type().name())
        .setStatus(value.status().name())
        .setAvailableBalance(value.availableBalance().longValue())
        .setReservedBalance(value.reservedBalance().longValue())
        .setCurrency(value.currency().name())
        .build();
  }

  default CreateAccountGrpcResponse toCreateAccountGrpcResponse(CreateAccountResult value) {
    return CreateAccountGrpcResponse.newBuilder().setAccount(toAccountResponse(value)).build();
  }

  default GetAccountsGrpcResponse toGetAccountsGrpcResponse(List<AccountResponse> values) {
    return GetAccountsGrpcResponse.newBuilder().addAllAccounts(values).build();
  }

  default GetAccountByIdGrpcResponse toGetAccountByIdGrpcResponse(AccountResponse response) {
    return GetAccountByIdGrpcResponse.newBuilder().setAccount(response).build();
  }

  default ReserveFundsForTransactionGrpcResponse toReserveFundsForTransactionGrpcResponse(
      ReserveFundsForTransactionResult result) {
    return ReserveFundsForTransactionGrpcResponse.newBuilder()
        .setStatus(result.status().name())
        .setMessage(result.message())
        .build();
  }
}
