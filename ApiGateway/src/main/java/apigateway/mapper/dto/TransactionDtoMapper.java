package apigateway.mapper.dto;

import apigateway.dto.transaction.CreateTransactionResponseDto;
import enums.common.Currency;
import enums.transaction.TransactionStatus;
import java.util.UUID;
import org.mapstruct.Mapper;
import transaction.contract.v1.CreateTransactionGrpcResponse;

@Mapper(componentModel = "spring")
public interface TransactionDtoMapper {
  default CreateTransactionResponseDto toCreateTransactionResponseDto(
      CreateTransactionGrpcResponse response) {
    return new CreateTransactionResponseDto(
        UUID.fromString(response.getTransactionId()),
        response.getMinorUnits(),
        Currency.valueOf(response.getCurrency()),
        TransactionStatus.valueOf(response.getStatus()));
  }
}
