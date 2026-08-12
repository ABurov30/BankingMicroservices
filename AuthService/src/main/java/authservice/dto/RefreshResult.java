package authservice.dto;

public record RefreshResult(
    String accessToken,
    String refreshToken,
    long accessTokenMinutesTtl,
    long refreshTokenDaysTtl) {}
