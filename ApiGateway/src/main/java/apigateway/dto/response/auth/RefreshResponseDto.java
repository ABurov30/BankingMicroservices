package apigateway.dto.response.auth;

public record RefreshResponseDto(
    String accessToken,
    String refreshToken,
    long accessTokenMinutesTtl,
    long refreshTokenDaysTtl) {}
