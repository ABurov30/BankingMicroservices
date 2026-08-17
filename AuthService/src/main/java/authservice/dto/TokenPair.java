package authservice.dto;

public record TokenPair(
    String accessToken,
    String refreshToken,
    long accessTokenMinutesTtl,
    long refreshTokenDaysTtl) {}
