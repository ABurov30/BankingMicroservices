package apigateway.mapper;

import apigateway.dto.card.CreateCardRequestDto;
import apigateway.dto.card.CreateCardResponseDto;
import card.contract.v1.CreateCardGrpcRequest;
import card.contract.v1.CreateCardGrpcResponse;
import enums.card.CardStatus;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CardMapper {
    default CreateCardGrpcRequest toCreateCardGrpcRequest (CreateCardRequestDto createCardRequestDto) {
        return  CreateCardGrpcRequest.newBuilder()
                .setAccountId(createCardRequestDto.accountId().toString())
                .build();
    }

    default CreateCardResponseDto toCreateCardResponseDto (CreateCardGrpcResponse response) {
        return new CreateCardResponseDto(
                UUID.fromString(response.getCardId()),
                UUID.fromString(response.getAccountId()),
                response.getPan(),
                CardStatus.valueOf(response.getStatus()),
                BigDecimal.valueOf(response.getDailyLimit()),
                BigDecimal.valueOf(response.getMonthlyLimit()),
                LocalDate.parse(response.getExpiresAt()).atStartOfDay()
        );
    }
}
