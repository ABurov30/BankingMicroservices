package apigateway.dto.user;

import apigateway.dto.card.GetCardByAccountIdResponseDto;
import java.util.List;

public record GetUserInfoAccountWithCardsResponseDto(
    GetUserInfoAccountResponseDto account, List<GetCardByAccountIdResponseDto> cards) {}
