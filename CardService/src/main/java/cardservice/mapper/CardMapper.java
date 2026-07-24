package cardservice.mapper;

import account.contract.v1.GetAccountsGrpcResponse;
import card.contract.v1.*;
import cardservice.dto.*;
import cardservice.entity.CardEntity;
import enums.card.CardStatus;
import kafkacontracts.account.AccountCreatedEventPayload;
import kafkacontracts.account.AccountFrozenEventPayload;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CardMapper {
    default CreatedCardCommand toCreateCardCommand(AccountCreatedEventPayload payload) {
        return new CreatedCardCommand(
                payload.getAccountId(),
                payload.getAuthUserId(),
                null
        );
    }

    default FreezeCardsCommand toFreezeCardsCommand(AccountFrozenEventPayload payload) {
        return new FreezeCardsCommand(payload.getAccountId());
    }

    @Mapping(target = "cardId", source = "id")
    @Mapping(target = "status", source = "cardStatus")
    CreateCardResult toCreateCardResult(CardEntity cardEntity);

    @Mapping(target = "cardId", source = "id")
    @Mapping(target = "status", source = "cardStatus")
    UpdateCardResult toUpdateCardResult(CardEntity cardEntity);

    @Mapping(target = "cardId", source = "id")
    @Mapping(target = "status", source = "cardStatus")
    GetCardResult toGetCardResult(CardEntity cardEntity);

    default CreatedCardCommand toCreateCardCommand(CreateCardGrpcRequest grpcRequest) {
        return new CreatedCardCommand(
                UUID.fromString(grpcRequest.getAccountId()),
                UUID.fromString(grpcRequest.getAuthUserId()),
                grpcRequest.getRole()
        );
    }

    default CreateCardGrpcResponse toCreateCardGrpcResponse(CreateCardResult cardResult) {
        return CreateCardGrpcResponse.newBuilder()
                .setCard(toCardResponse(cardResult))
                .build();
    }

    default UpdateCardCommand toUpdateCardCommand(UpdateCardGrpcRequest grpcRequest) {
        return new UpdateCardCommand(
                UUID.fromString(grpcRequest.getCardId()),
                CardStatus.valueOf(grpcRequest.getStatus()),
                BigDecimal.valueOf(grpcRequest.getDailyLimit()),
                BigDecimal.valueOf(grpcRequest.getMonthlyLimit()),
                UUID.fromString(grpcRequest.getAuthUserId()),
                grpcRequest.getRole()
        );
    }

    default UpdateCardGrpcResponse toUpdateCardGrpcResponse(UpdateCardResult cardResult) {
        return UpdateCardGrpcResponse.newBuilder()
                .setCard(toCardResponse(cardResult))
                .build();
    }

    default GetCardsByAccountIdCommand toGetCardsByAccountIdCommand(GetCardByAccountIdGrpcRequest grpcRequest) {
        return new GetCardsByAccountIdCommand(UUID.fromString(grpcRequest.getAccountId()));
    }

    default CardResponse toCardResponse(CreateCardResult cardResult) {
        return CardResponse.newBuilder()
                .setCardId(cardResult.cardId().toString())
                .setAccountId(cardResult.accountId().toString())
                .setPan(cardResult.pan())
                .setStatus(cardResult.status().name())
                .setDailyLimit(cardResult.dailyLimit().longValue())
                .setMonthlyLimit(cardResult.monthlyLimit().longValue())
                .setExpiresAt(cardResult.expiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    default CardResponse toCardResponse(UpdateCardResult cardResult) {
        return CardResponse.newBuilder()
                .setCardId(cardResult.cardId().toString())
                .setAccountId(cardResult.accountId().toString())
                .setPan(cardResult.pan())
                .setStatus(cardResult.status().name())
                .setDailyLimit(cardResult.dailyLimit().longValue())
                .setMonthlyLimit(cardResult.monthlyLimit().longValue())
                .setExpiresAt(cardResult.expiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    default CardResponse toCardResponse(GetCardResult cardResult) {
        return CardResponse.newBuilder()
                .setCardId(cardResult.cardId().toString())
                .setAccountId(cardResult.accountId().toString())
                .setPan(cardResult.pan())
                .setStatus(cardResult.status().name())
                .setDailyLimit(cardResult.dailyLimit().longValue())
                .setMonthlyLimit(cardResult.monthlyLimit().longValue())
                .setExpiresAt(cardResult.expiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    default GetCardsGrpcResponse toGetCardsGrpcResponse(List<CardResponse> cardResponseList) {
        return GetCardsGrpcResponse.newBuilder()
                .addAllCards(cardResponseList)
                .build();
    }
}
