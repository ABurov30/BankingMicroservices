package authservice.dto;

public record SocialLoginResult(
    String accessToken,
    String refreshToken,
    long accessTokenMinutesTtl,
    long refreshTokenDaysTtl) {}
