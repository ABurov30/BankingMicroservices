package apigateway.dto.response.account;

import apigateway.dto.response.card.GetCardByAccountIdResponseDto;
import java.util.List;

public record GetAccountWithCardsResponseDto(
    GetAccountResponseDto account, List<GetCardByAccountIdResponseDto> cards) {}
