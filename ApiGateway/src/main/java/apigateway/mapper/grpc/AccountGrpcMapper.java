package apigateway.mapper.grpc;

import account.contract.v1.CreateAccountGrpcRequest;
import apigateway.dto.account.CreateAccountRequestDto;
import org.mapstruct.Mapper;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AccountGrpcMapper {
    default CreateAccountGrpcRequest toCreateAccountGrpcRequest(CreateAccountRequestDto request, UUID authUserId) {
        return CreateAccountGrpcRequest.newBuilder().setOwnerUserId(request.ownerUserId().toString())
                .setAuthUserId(authUserId.toString()).setType(request.type().name()).setCurrency(request.currency().name()).build();
    }
}
