package apigateway.mapper.dto;

import apigateway.dto.card.CreateCardResponseDto;
import apigateway.dto.card.GetCardByAccountIdResponseDto;
import apigateway.dto.card.UpdateCardResponseDto;
import card.contract.v1.CardResponse;
import card.contract.v1.CreateCardGrpcResponse;
import card.contract.v1.UpdateCardGrpcResponse;
import enums.card.CardStatus;
import enums.common.Currency;
import java.time.LocalDateTime;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CardDtoMapper {
  default CreateCardResponseDto toCreateCardResponseDto(CreateCardGrpcResponse response) {
    CardResponse card = response.getCard();
    Currency currency = toCurrency(card.getCurrency());
    return new CreateCardResponseDto(
        UUID.fromString(card.getCardId()),
        UUID.fromString(card.getAccountId()),
        card.getPan(),
        CardStatus.valueOf(card.getStatus()),
        card.getDailyLimitMinorUnits(),
        card.getMonthlyLimitMinorUnits(),
        LocalDateTime.parse(card.getExpiresAt()),
        card.getSpendDailyLimitMinorUnits(),
        card.getSpendMonthlyLimitMinorUnits(),
        currency);
  }

  default UpdateCardResponseDto toUpdateCardResponseDto(UpdateCardGrpcResponse response) {
    CardResponse card = response.getCard();
    Currency currency = toCurrency(card.getCurrency());
    return new UpdateCardResponseDto(
        UUID.fromString(card.getCardId()),
        UUID.fromString(card.getAccountId()),
        card.getPan(),
        CardStatus.valueOf(card.getStatus()),
        card.getDailyLimitMinorUnits(),
        card.getMonthlyLimitMinorUnits(),
        LocalDateTime.parse(card.getExpiresAt()),
        card.getSpendDailyLimitMinorUnits(),
        card.getSpendMonthlyLimitMinorUnits(),
        currency);
  }

  default GetCardByAccountIdResponseDto toGetCardByAccountIdResponseDto(CardResponse card) {
    Currency currency = toCurrency(card.getCurrency());
    return new GetCardByAccountIdResponseDto(
        UUID.fromString(card.getCardId()),
        UUID.fromString(card.getAccountId()),
        card.getPan(),
        CardStatus.valueOf(card.getStatus()),
        card.getDailyLimitMinorUnits(),
        card.getMonthlyLimitMinorUnits(),
        LocalDateTime.parse(card.getExpiresAt()),
        card.getSpendDailyLimitMinorUnits(),
        card.getSpendMonthlyLimitMinorUnits(),
        currency);
  }

  private Currency toCurrency(String currency) {
    return currency == null || currency.isBlank() ? null : Currency.valueOf(currency);
  }
}
