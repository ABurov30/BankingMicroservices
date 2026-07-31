package cardservice.mapper.command;

import card.contract.v1.CreateCardGrpcRequest;
import card.contract.v1.GetCardByAccountIdGrpcRequest;
import card.contract.v1.UpdateCardGrpcRequest;
import cardservice.dto.CreatedCardCommand;
import cardservice.dto.FreezeCardsCommand;
import cardservice.dto.GetCardsByAccountIdCommand;
import cardservice.dto.UnfreezeCardsCommand;
import cardservice.dto.UpdateCardCommand;
import enums.card.CardStatus;
import kafkacontracts.account.AccountCreatedEventPayload;
import kafkacontracts.account.AccountFrozenEventPayload;
import kafkacontracts.account.AccountUnfrozenEventPayload;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CardCommandMapper {
    default CreatedCardCommand toCreateCardCommand(AccountCreatedEventPayload payload) { return new CreatedCardCommand(payload.getAccountId(), payload.getAuthUserId(), null); }
    default FreezeCardsCommand toFreezeCardsCommand(AccountFrozenEventPayload payload) { return new FreezeCardsCommand(payload.getAccountId()); }
    default UnfreezeCardsCommand toUnfreezeCardsCommand(AccountUnfrozenEventPayload payload) { return new UnfreezeCardsCommand(payload.getAccountId()); }
    default CreatedCardCommand toCreateCardCommand(CreateCardGrpcRequest request) { return new CreatedCardCommand(UUID.fromString(request.getAccountId()), UUID.fromString(request.getAuthUserId()), request.getRole()); }
    default UpdateCardCommand toUpdateCardCommand(UpdateCardGrpcRequest request) { return new UpdateCardCommand(UUID.fromString(request.getCardId()), CardStatus.valueOf(request.getStatus()), BigDecimal.valueOf(request.getDailyLimit()), BigDecimal.valueOf(request.getMonthlyLimit()), UUID.fromString(request.getAuthUserId()), request.getRole()); }
    default GetCardsByAccountIdCommand toGetCardsByAccountIdCommand(GetCardByAccountIdGrpcRequest request) { return new GetCardsByAccountIdCommand(UUID.fromString(request.getAccountId())); }
}
