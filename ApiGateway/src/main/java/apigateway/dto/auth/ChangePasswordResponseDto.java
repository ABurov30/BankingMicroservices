package apigateway.dto.auth;

public record ChangePasswordResponseDto(String refreshToken, long refreshTokenDaysTtl) {}
