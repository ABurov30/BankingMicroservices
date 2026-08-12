package accountservice.mapper.result;

import accountservice.dto.GetAccountResult;
import accountservice.entity.AccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountResultMapper {
  @Mapping(target = "accountId", source = "id")
  @Mapping(target = "authUserId", source = "ownerAuthUserId")
  @Mapping(target = "type", source = "accountType")
  @Mapping(target = "status", source = "accountStatus")
  @Mapping(target = "currency", source = "currency.name")
  GetAccountResult toGetAccountResult(AccountEntity accountEntity);
}
