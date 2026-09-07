package apigateway.dto.response.auth;

public record ChangePasswordResponseDto(String refreshToken, long refreshTokenDaysTtl) {}
