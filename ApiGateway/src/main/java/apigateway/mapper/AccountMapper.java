package apigateway.mapper;

import account.contract.v1.CreateAccountGrpcRequest;
import account.contract.v1.CreateAccountGrpcResponse;
import apigateway.dto.account.CreateAccountRequestDto;
import apigateway.dto.account.CreateAccountResponseDto;
import auth.contract.v1.SignupAuthGrpcRequest;
import enums.account.AccountCurrency;
import enums.account.AccountStatus;
import enums.account.AccountType;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    default CreateAccountGrpcRequest toCreateAccountGrpcRequest(CreateAccountRequestDto request) {
        return CreateAccountGrpcRequest.newBuilder()
                .setOwnerUserId(request.ownerUserId().toString())
                .setType(request.type().name())
                .setCurrency(request.currency().name())
                .build();
    }

    default CreateAccountResponseDto toCreateAccountResponseDto(CreateAccountGrpcResponse response) {
        return new CreateAccountResponseDto(
                UUID.fromString(response.getAccountId()),
                UUID.fromString(response.getOwnerUserId()),
                response.getAccountNumber(),
                AccountType.valueOf(response.getType()),
                AccountStatus.valueOf(response.getStatus()),
                BigDecimal.valueOf(response.getAvailableBalance(), 2),
                BigDecimal.valueOf(response.getReservedBalance(), 2),
                AccountCurrency.valueOf(response.getCurrency())
        );
    }
}
