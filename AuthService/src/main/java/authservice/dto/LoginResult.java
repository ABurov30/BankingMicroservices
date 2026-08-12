package authservice.dto;

public record LoginResult(
    String accessToken,
    String refreshToken,
    long accessTokenMinutesTtl,
    long refreshTokenDaysTtl) {}
