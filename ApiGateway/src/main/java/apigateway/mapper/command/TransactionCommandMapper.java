package apigateway.mapper.command;

import apigateway.dto.command.transaction.GetTransactionByMeCommandDto;
import apigateway.dto.command.transaction.GetTransactionByUserIdCommandDto;
import enums.auth.Roles;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionCommandMapper {
  default GetTransactionByUserIdCommandDto toGetTransactionByUserIdCommandDto(
      UUID ownerUserId, UUID authUserId, Roles role) {
    return new GetTransactionByUserIdCommandDto(ownerUserId, authUserId, role);
  }

  default GetTransactionByMeCommandDto toGetTransactionByMeCommandDto(UUID authUserId, Roles role) {
    return new GetTransactionByMeCommandDto(authUserId, role);
  }
}
