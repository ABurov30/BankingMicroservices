package apigateway.mapper.result;

import apigateway.dto.response.transaction.CreateTransactionResponseDto;
import enums.common.Currency;
import enums.transaction.TransactionStatus;
import java.util.UUID;
import org.mapstruct.Mapper;
import transaction.contract.v1.CreateTransactionGrpcResponse;

@Mapper(componentModel = "spring")
public interface TransactionResultMapper {
  default CreateTransactionResponseDto toCreateTransactionResponseDto(
      CreateTransactionGrpcResponse response) {
    return new CreateTransactionResponseDto(
        UUID.fromString(response.getTransactionId()),
        response.getMinorUnits(),
        Currency.valueOf(response.getCurrency()),
        TransactionStatus.valueOf(response.getStatus()));
  }
}
