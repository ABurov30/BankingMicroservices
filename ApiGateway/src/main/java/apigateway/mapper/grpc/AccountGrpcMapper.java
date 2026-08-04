package apigateway.mapper.grpc;

import account.contract.v1.CreateAccountGrpcRequest;
import account.contract.v1.GetAccountByIdGrpcRequest;
import account.contract.v1.UpdateAccountBalanceGrpcRequest;
import apigateway.dto.account.CreateAccountRequestDto;
import apigateway.dto.account.GetAccountByIdRequestDto;
import apigateway.dto.account.UpdateAccountBalanceRequestDto;
import org.mapstruct.Mapper;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AccountGrpcMapper {
    default CreateAccountGrpcRequest toCreateAccountGrpcRequest(CreateAccountRequestDto request, UUID authUserId) {
        return CreateAccountGrpcRequest.newBuilder().setOwnerUserId(request.ownerUserId().toString())
                .setAuthUserId(authUserId.toString()).setType(request.type().name()).setCurrency(request.currency().name()).build();
    }

    default GetAccountByIdGrpcRequest toGetAccountByIdGrpcRequest(GetAccountByIdRequestDto request) {
        return GetAccountByIdGrpcRequest.newBuilder()
                .setAccountId(request.accountId().toString())
                .build();
    }

    default UpdateAccountBalanceGrpcRequest toUpdateAccountBalanceGrpcRequest (UpdateAccountBalanceRequestDto request) {
        return UpdateAccountBalanceGrpcRequest.newBuilder()
                .setAccountId(request.accountId().toString())
                .setAmount(request.amount().longValue())
                .build();
    }
}
