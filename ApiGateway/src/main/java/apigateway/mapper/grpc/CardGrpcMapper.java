package apigateway.mapper.grpc;

import apigateway.dto.account.GetAccountResponseDto;
import apigateway.dto.card.CreateCardRequestDto;
import apigateway.dto.card.UpdateCardRequestDto;
import card.contract.v1.CreateCardGrpcRequest;
import card.contract.v1.UpdateCardGrpcRequest;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CardGrpcMapper {
  default CreateCardGrpcRequest toCreateCardGrpcRequest(
      CreateCardRequestDto request, UUID authUserId, String role, GetAccountResponseDto account) {
    return CreateCardGrpcRequest.newBuilder()
        .setAccountId(request.accountId().toString())
        .setAuthUserId(authUserId.toString())
        .setRole(role == null ? "" : role)
        .setCurrency(account.currency().name())
        .build();
  }

  default UpdateCardGrpcRequest toUpdateCardGrpcRequest(
      UpdateCardRequestDto request, UUID authUserId, String role) {
    return UpdateCardGrpcRequest.newBuilder()
        .setCardId(request.cardId().toString())
        .setStatus(request.status().name())
        .setDailyLimitMinorUnits(request.dailyLimitMinorUnits().longValue())
        .setMonthlyLimitMinorUnits(request.monthlyLimitMinorUnits().longValue())
        .setAuthUserId(authUserId.toString())
        .setRole(role == null ? "" : role)
        .build();
  }
}
