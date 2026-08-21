package cardservice.mapper.result;

import cardservice.dto.*;
import cardservice.entity.CardEntity;
import enums.common.Currency;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CardResultMapper {
  @Mapping(target = "cardId", source = "value.id")
  @Mapping(target = "status", source = "value.cardStatus")
  @Mapping(target = "currency", source = "currency")
  CreateCardResult toCreateCardResult(CardEntity value, Currency currency);

  @Mapping(target = "cardId", source = "value.id")
  @Mapping(target = "status", source = "value.cardStatus")
  @Mapping(target = "currency", source = "currency")
  UpdateCardResult toUpdateCardResult(CardEntity value, Currency currency);

  @Mapping(target = "cardId", source = "value.id")
  @Mapping(target = "status", source = "value.cardStatus")
  @Mapping(target = "currency", source = "currency")
  GetCardResult toGetCardResult(CardEntity value, Currency currency);
}
