package apigateway.mapper;

import apigateway.dto.card.CreateCardRequestDto;
import apigateway.dto.card.CreateCardResponseDto;
import apigateway.dto.card.GetCardByAccountIdResponseDto;
import apigateway.dto.card.UpdateCardRequestDto;
import apigateway.dto.card.UpdateCardResponseDto;
import card.contract.v1.CardResponse;
import card.contract.v1.CreateCardGrpcRequest;
import card.contract.v1.CreateCardGrpcResponse;
import card.contract.v1.UpdateCardGrpcRequest;
import card.contract.v1.UpdateCardGrpcResponse;
import enums.card.CardStatus;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CardMapper {
    default CreateCardGrpcRequest toCreateCardGrpcRequest (
            CreateCardRequestDto createCardRequestDto,
            UUID authUserId,
            String role
    ) {
        return  CreateCardGrpcRequest.newBuilder()
                .setAccountId(createCardRequestDto.accountId().toString())
                .setAuthUserId(authUserId.toString())
                .setRole(role == null ? "" : role)
                .build();
    }

    default CreateCardResponseDto toCreateCardResponseDto (CreateCardGrpcResponse response) {
        CardResponse card = response.getCard();

        return new CreateCardResponseDto(
                UUID.fromString(card.getCardId()),
                UUID.fromString(card.getAccountId()),
                card.getPan(),
                CardStatus.valueOf(card.getStatus()),
                BigDecimal.valueOf(card.getDailyLimit()),
                BigDecimal.valueOf(card.getMonthlyLimit()),
                LocalDateTime.parse(card.getExpiresAt())
        );
    }

    default UpdateCardGrpcRequest toUpdateCardGrpcRequest(
            UpdateCardRequestDto request,
            UUID authUserId,
            String role
    ) {
        return UpdateCardGrpcRequest.newBuilder()
                .setCardId(request.cardId().toString())
                .setStatus(request.status().name())
                .setDailyLimit(request.dailyLimit().longValue())
                .setMonthlyLimit(request.monthlyLimit().longValue())
                .setAuthUserId(authUserId.toString())
                .setRole(role == null ? "" : role)
                .build();
    }

    default UpdateCardResponseDto toUpdateCardResponseDto(UpdateCardGrpcResponse response) {
        CardResponse card = response.getCard();

        return new UpdateCardResponseDto(
                UUID.fromString(card.getCardId()),
                UUID.fromString(card.getAccountId()),
                card.getPan(),
                CardStatus.valueOf(card.getStatus()),
                BigDecimal.valueOf(card.getDailyLimit()),
                BigDecimal.valueOf(card.getMonthlyLimit()),
                LocalDateTime.parse(card.getExpiresAt())
        );
    }

    default GetCardByAccountIdResponseDto toGetCardByAccountIdResponseDto(CardResponse card) {
        return new GetCardByAccountIdResponseDto(
                UUID.fromString(card.getCardId()),
                UUID.fromString(card.getAccountId()),
                card.getPan(),
                CardStatus.valueOf(card.getStatus()),
                BigDecimal.valueOf(card.getDailyLimit()),
                BigDecimal.valueOf(card.getMonthlyLimit()),
                LocalDateTime.parse(card.getExpiresAt())
        );
    }
}
