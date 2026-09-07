package apigateway.dto.response.user;

import java.util.List;

public record GetUserInfoWithAccountResponseDto(
    GetUserInfoResponseDto userInfo, List<GetUserInfoAccountWithCardsResponseDto> accounts) {}
