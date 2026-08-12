package apigateway.dto.account;

import apigateway.dto.card.GetCardByAccountIdResponseDto;
import java.util.List;

public record GetAccountWithCardsResponseDto(
    GetAccountResponseDto account, List<GetCardByAccountIdResponseDto> cards) {}
