package cardservice.mapper.grpc;

import card.contract.v1.CardResponse;
import card.contract.v1.CreateCardGrpcResponse;
import card.contract.v1.GetCardsGrpcResponse;
import card.contract.v1.UpdateCardGrpcResponse;
import cardservice.dto.CreateCardResult;
import cardservice.dto.GetCardResult;
import cardservice.dto.UpdateCardResult;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CardGrpcMapper {
  default CreateCardGrpcResponse toCreateCardGrpcResponse(CreateCardResult value) {
    return CreateCardGrpcResponse.newBuilder().setCard(toCardResponse(value)).build();
  }

  default UpdateCardGrpcResponse toUpdateCardGrpcResponse(UpdateCardResult value) {
    return UpdateCardGrpcResponse.newBuilder().setCard(toCardResponse(value)).build();
  }

  default CardResponse toCardResponse(CreateCardResult value) {
    return CardResponse.newBuilder()
        .setCardId(value.cardId().toString())
        .setAccountId(value.accountId().toString())
        .setPan(value.pan())
        .setStatus(value.status().name())
        .setDailyLimit(value.dailyLimit().longValue())
        .setMonthlyLimit(value.monthlyLimit().longValue())
        .setExpiresAt(value.expiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .build();
  }

  default CardResponse toCardResponse(UpdateCardResult value) {
    return CardResponse.newBuilder()
        .setCardId(value.cardId().toString())
        .setAccountId(value.accountId().toString())
        .setPan(value.pan())
        .setStatus(value.status().name())
        .setDailyLimit(value.dailyLimit().longValue())
        .setMonthlyLimit(value.monthlyLimit().longValue())
        .setExpiresAt(value.expiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .build();
  }

  default CardResponse toCardResponse(GetCardResult value) {
    return CardResponse.newBuilder()
        .setCardId(value.cardId().toString())
        .setAccountId(value.accountId().toString())
        .setPan(value.pan())
        .setStatus(value.status().name())
        .setDailyLimit(value.dailyLimit().longValue())
        .setMonthlyLimit(value.monthlyLimit().longValue())
        .setExpiresAt(value.expiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .build();
  }

  default GetCardsGrpcResponse toGetCardsGrpcResponse(List<CardResponse> values) {
    return GetCardsGrpcResponse.newBuilder().addAllCards(values).build();
  }
}
