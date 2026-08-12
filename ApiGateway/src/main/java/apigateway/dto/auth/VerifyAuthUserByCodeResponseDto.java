package apigateway.dto.auth;

public record VerifyAuthUserByCodeResponseDto(
    String accessToken,
    String refreshToken,
    long accessTokenMinutesTtl,
    long refreshTokenDaysTtl) {}
