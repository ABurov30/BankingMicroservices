package apigateway.mapper.grpc;

import apigateway.dto.card.CreateCardRequestDto;
import apigateway.dto.card.UpdateCardRequestDto;
import card.contract.v1.CreateCardGrpcRequest;
import card.contract.v1.UpdateCardGrpcRequest;
import org.mapstruct.Mapper;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CardGrpcMapper {
    default CreateCardGrpcRequest toCreateCardGrpcRequest(CreateCardRequestDto request, UUID authUserId, String role) {
        return CreateCardGrpcRequest.newBuilder().setAccountId(request.accountId().toString()).setAuthUserId(authUserId.toString()).setRole(role == null ? "" : role).build();
    }

    default UpdateCardGrpcRequest toUpdateCardGrpcRequest(UpdateCardRequestDto request, UUID authUserId, String role) {
        return UpdateCardGrpcRequest.newBuilder().setCardId(request.cardId().toString()).setStatus(request.status().name()).setDailyLimit(request.dailyLimit().longValue()).setMonthlyLimit(request.monthlyLimit().longValue()).setAuthUserId(authUserId.toString()).setRole(role == null ? "" : role).build();
    }
}
