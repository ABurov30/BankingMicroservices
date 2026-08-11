package apigateway.mapper.grpc;

import apigateway.dto.transaction.CreateTransactionRequestDto;
import org.mapstruct.Mapper;
import transaction.contract.v1.CreateTransactionGrpcRequest;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface TransactionGrpcMapper {

    default CreateTransactionGrpcRequest toCreateTransactionGrpcRequest(
            CreateTransactionRequestDto request,
            UUID sourceAuthUserId,
            UUID targetAuthUserId
    ) {
        return CreateTransactionGrpcRequest.newBuilder()
                .setSourceAccountId(request.sourceAccountId().toString())
                .setTargetAccountId(request.targetAccountId().toString())
                .setAmount(request.amount().longValue())
                .setCurrency(request.currency().name())
                .setIdempotencyKey(request.idempotencyKey().toString())
                .setSourceAuthUserId(sourceAuthUserId.toString())
                .setSourceTargetAuthUserId(targetAuthUserId.toString())
                .build();
    }
}
