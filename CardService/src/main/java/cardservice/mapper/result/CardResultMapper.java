package cardservice.mapper.result;

import cardservice.dto.*;
import cardservice.entity.CardEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CardResultMapper {
    @Mapping(target = "cardId", source = "id") @Mapping(target = "status", source = "cardStatus") CreateCardResult toCreateCardResult(CardEntity value);
    @Mapping(target = "cardId", source = "id") @Mapping(target = "status", source = "cardStatus") UpdateCardResult toUpdateCardResult(CardEntity value);
    @Mapping(target = "cardId", source = "id") @Mapping(target = "status", source = "cardStatus") GetCardResult toGetCardResult(CardEntity value);
}
