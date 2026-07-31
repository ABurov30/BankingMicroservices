package accountservice.mapper.grpc;

import account.contract.v1.AccountResponse;
import account.contract.v1.CreateAccountGrpcResponse;
import account.contract.v1.GetAccountsGrpcResponse;
import accountservice.dto.CreateAccountResult;
import accountservice.dto.GetAccountResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccountGrpcMapper {
    default AccountResponse toAccountResponse(GetAccountResult value) { return AccountResponse.newBuilder().setAccountId(value.accountId().toString()).setOwnerUserId(value.ownerUserId().toString()).setAccountNumber(value.accountNumber()).setType(value.type().name()).setStatus(value.status().name()).setAvailableBalance(value.availableBalance().longValue()).setReservedBalance(value.reservedBalance().longValue()).setCurrency(value.currency().name()).build(); }
    default AccountResponse toAccountResponse(CreateAccountResult value) { return AccountResponse.newBuilder().setAccountId(value.accountId().toString()).setOwnerUserId(value.ownerUserId().toString()).setAccountNumber(value.accountNumber()).setType(value.type().name()).setStatus(value.status().name()).setAvailableBalance(value.availableBalance().longValue()).setReservedBalance(value.reservedBalance().longValue()).setCurrency(value.currency().name()).build(); }
    default CreateAccountGrpcResponse toCreateAccountGrpcResponse(CreateAccountResult value) { return CreateAccountGrpcResponse.newBuilder().setAccount(toAccountResponse(value)).build(); }
    default GetAccountsGrpcResponse toGetAccountsGrpcResponse(List<AccountResponse> values) { return GetAccountsGrpcResponse.newBuilder().addAllAccounts(values).build(); }
}
