package apigateway.dto.response.auth;

public record ResetPasswordsResponseDto(
    String accessToken,
    String refreshToken,
    long accessTokenMinutesTtl,
    long refreshTokenDaysTtl) {}
