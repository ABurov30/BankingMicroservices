package cardservice.mapper;

import card.contract.v1.CreateCardGrpcRequest;
import card.contract.v1.CreateCardGrpcResponse;
import cardservice.dto.CreateCardResult;
import cardservice.entity.CardEntity;
import cardservice.dto.CreatedCardCommand;
import kafkacontracts.account.AccountCreatedEventPayload;
import org.mapstruct.Mapper;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Mapper(componentModel = "spring"
)
public interface CardMapper {
    CreatedCardCommand toCreateCardCommand(AccountCreatedEventPayload accountCreatedEventPayload);

    default CreateCardResult toCreateCardResult(CardEntity cardEntity) {
        return new CreateCardResult(
                cardEntity.getId(),
                cardEntity.getAccountId(),
                cardEntity.getPan(),
                cardEntity.getCardStatus(),
                cardEntity.getDailyLimit(),
                cardEntity.getMonthlyLimit(),
                cardEntity.getExpiresAt()
        );
    }

    default CreatedCardCommand toCreateCardCommand(CreateCardGrpcRequest grpcRequest) {
        return new  CreatedCardCommand (
                UUID.fromString(grpcRequest.getAccountId())
        );
    }

    default CreateCardGrpcResponse toCreateCardGrpcResponse(CreateCardResult cardResult) {
        return CreateCardGrpcResponse.newBuilder()
                .setCardId(cardResult.cardId().toString())
                .setAccountId(cardResult.accountId().toString())
                .setPan(cardResult.pan())
                .setStatus(cardResult.status().name())
                .setDailyLimit(cardResult.dailyLimit().longValue())
                .setMonthlyLimit(cardResult.monthlyLimit().longValue())
                .setExpiresAt(cardResult.expiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}
