package apigateway.mapper.dto;

import apigateway.dto.card.CreateCardResponseDto;
import apigateway.dto.card.GetCardByAccountIdResponseDto;
import apigateway.dto.card.UpdateCardResponseDto;
import card.contract.v1.CardResponse;
import card.contract.v1.CreateCardGrpcResponse;
import card.contract.v1.UpdateCardGrpcResponse;
import enums.card.CardStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CardDtoMapper {
  default CreateCardResponseDto toCreateCardResponseDto(CreateCardGrpcResponse response) {
    CardResponse card = response.getCard();
    return new CreateCardResponseDto(
        UUID.fromString(card.getCardId()),
        UUID.fromString(card.getAccountId()),
        card.getPan(),
        CardStatus.valueOf(card.getStatus()),
        BigDecimal.valueOf(card.getDailyLimit()),
        BigDecimal.valueOf(card.getMonthlyLimit()),
        LocalDateTime.parse(card.getExpiresAt()));
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
        LocalDateTime.parse(card.getExpiresAt()));
  }

  default GetCardByAccountIdResponseDto toGetCardByAccountIdResponseDto(CardResponse card) {
    return new GetCardByAccountIdResponseDto(
        UUID.fromString(card.getCardId()),
        UUID.fromString(card.getAccountId()),
        card.getPan(),
        CardStatus.valueOf(card.getStatus()),
        BigDecimal.valueOf(card.getDailyLimit()),
        BigDecimal.valueOf(card.getMonthlyLimit()),
        LocalDateTime.parse(card.getExpiresAt()));
  }
}
