package cardservice.mapper.command;

import card.contract.v1.CreateCardGrpcRequest;
import card.contract.v1.GetCardByAccountIdGrpcRequest;
import card.contract.v1.ReserveLimitsForTransactionGrpcRequest;
import card.contract.v1.UpdateCardGrpcRequest;
import cardservice.dto.CompensateLimitsForTransactionCommand;
import cardservice.dto.CreatedCardCommand;
import cardservice.dto.FreezeCardsCommand;
import cardservice.dto.GetCardsByAccountIdCommand;
import cardservice.dto.MarkLimitReservationAsReleasedCommand;
import cardservice.dto.ReserveLimitsForTransactionCommand;
import cardservice.dto.UnfreezeCardsCommand;
import cardservice.dto.UpdateCardCommand;
import enums.card.CardStatus;
import java.math.BigDecimal;
import java.util.UUID;
import kafkacontracts.account.AccountCreatedEventPayload;
import kafkacontracts.account.AccountFrozenEventPayload;
import kafkacontracts.account.AccountUnfrozenEventPayload;
import kafkacontracts.account.TransactionCompensatedEventPayload;
import kafkacontracts.account.TransactionCompletedEventPayload;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CardCommandMapper {
  default CreatedCardCommand toCreateCardCommand(AccountCreatedEventPayload payload) {
    return new CreatedCardCommand(
        payload.getAccountId(), payload.getAuthUserId(), payload.getAccountNumber(), null);
  }

  default CreatedCardCommand toCreateCardCommand(CreateCardGrpcRequest request) {
    return new CreatedCardCommand(
        UUID.fromString(request.getAccountId()),
        UUID.fromString(request.getAuthUserId()),
        null,
        request.getRole());
  }

  default FreezeCardsCommand toFreezeCardsCommand(AccountFrozenEventPayload payload) {
    return new FreezeCardsCommand(
        payload.getAccountId(), payload.getAuthUserId(), payload.getAccountNumber());
  }

  default UnfreezeCardsCommand toUnfreezeCardsCommand(AccountUnfrozenEventPayload payload) {
    return new UnfreezeCardsCommand(
        payload.getAccountId(), payload.getAuthUserId(), payload.getAccountNumber());
  }

  default UpdateCardCommand toUpdateCardCommand(UpdateCardGrpcRequest request) {
    return new UpdateCardCommand(
        UUID.fromString(request.getCardId()),
        CardStatus.valueOf(request.getStatus()),
        BigDecimal.valueOf(request.getDailyLimit()),
        BigDecimal.valueOf(request.getMonthlyLimit()),
        UUID.fromString(request.getAuthUserId()),
        request.getRole());
  }

  default GetCardsByAccountIdCommand toGetCardsByAccountIdCommand(
      GetCardByAccountIdGrpcRequest request) {
    return new GetCardsByAccountIdCommand(UUID.fromString(request.getAccountId()));
  }

  default ReserveLimitsForTransactionCommand toReserveLimitsForTransactionCommand(
      ReserveLimitsForTransactionGrpcRequest request) {
    return new ReserveLimitsForTransactionCommand(
        UUID.fromString(request.getSourceCardId()),
        BigDecimal.valueOf(request.getAmount()),
        UUID.fromString(request.getTransactionId()),
        UUID.fromString(request.getSourceAuthUserId()));
  }

  default CompensateLimitsForTransactionCommand toCompensateLimitsForTransactionCommand(
      TransactionCompensatedEventPayload payload) {
    return new CompensateLimitsForTransactionCommand(payload.getTransactionId());
  }

  default MarkLimitReservationAsReleasedCommand toMarkLimitReservationAsReleasedCommand(
      TransactionCompletedEventPayload payload) {
    return new MarkLimitReservationAsReleasedCommand(payload.getTransactionId());
  }
}
