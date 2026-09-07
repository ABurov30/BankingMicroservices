package apigateway.dto.command.transaction;

import apigateway.dto.request.transaction.CreateTransactionRequestDto;
import apigateway.dto.response.user.GetUserInfoResponseDto;

public record CreateTransactionWithSourceUserInfoDto(
    CreateTransactionRequestDto createTransactionRequest, GetUserInfoResponseDto sourceUserInfo) {}
