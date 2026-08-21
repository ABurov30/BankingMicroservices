package apigateway.mapper.dto;

import apigateway.dto.card.CreateCardResponseDto;
import apigateway.dto.card.GetCardByAccountIdResponseDto;
import apigateway.dto.card.UpdateCardResponseDto;
import card.contract.v1.CardResponse;
import card.contract.v1.CreateCardGrpcResponse;
import card.contract.v1.UpdateCardGrpcResponse;
import enums.card.CardStatus;
import enums.common.Currency;
import java.math.BigDecimal;
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
        toAmount(card.getDailyLimitMinorUnits(), currency),
        toAmount(card.getMonthlyLimitMinorUnits(), currency),
        LocalDateTime.parse(card.getExpiresAt()),
        toAmount(card.getSpendDailyLimitMinorUnits(), currency),
        toAmount(card.getSpendMonthlyLimitMinorUnits(), currency),
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
        toAmount(card.getDailyLimitMinorUnits(), currency),
        toAmount(card.getMonthlyLimitMinorUnits(), currency),
        LocalDateTime.parse(card.getExpiresAt()),
        toAmount(card.getSpendDailyLimitMinorUnits(), currency),
        toAmount(card.getSpendMonthlyLimitMinorUnits(), currency),
        currency);
  }

  default GetCardByAccountIdResponseDto toGetCardByAccountIdResponseDto(CardResponse card) {
    Currency currency = toCurrency(card.getCurrency());
    return new GetCardByAccountIdResponseDto(
        UUID.fromString(card.getCardId()),
        UUID.fromString(card.getAccountId()),
        card.getPan(),
        CardStatus.valueOf(card.getStatus()),
        toAmount(card.getDailyLimitMinorUnits(), currency),
        toAmount(card.getMonthlyLimitMinorUnits(), currency),
        LocalDateTime.parse(card.getExpiresAt()),
        toAmount(card.getSpendDailyLimitMinorUnits(), currency),
        toAmount(card.getSpendMonthlyLimitMinorUnits(), currency),
        currency);
  }

  private Currency toCurrency(String currency) {
    return currency == null || currency.isBlank() ? null : Currency.valueOf(currency);
  }

  private BigDecimal toAmount(long amount, Currency currency) {
    return currency == null
        ? BigDecimal.valueOf(amount)
        : BigDecimal.valueOf(amount).movePointLeft(currency.getMinorUnit());
  }
}
