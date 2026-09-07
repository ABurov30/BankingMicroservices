package apigateway.dto.response.user;

import apigateway.dto.response.card.GetCardByAccountIdResponseDto;
import java.util.List;

public record GetUserInfoAccountWithCardsResponseDto(
    GetUserInfoAccountResponseDto account, List<GetCardByAccountIdResponseDto> cards) {}
