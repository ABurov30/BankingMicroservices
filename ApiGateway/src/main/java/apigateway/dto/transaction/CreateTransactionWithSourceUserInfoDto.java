package apigateway.dto.transaction;

import apigateway.dto.user.GetUserInfoResponseDto;

public record CreateTransactionWithSourceUserInfoDto(
        CreateTransactionRequestDto createTransactionRequest,
        GetUserInfoResponseDto sourceUserInfo
        ) {
}
