package authservice.dto;

public record VerifyAuthUserByCodeResult(
    String accessToken,
    String refreshToken,
    long accessTokenMinutesTtl,
    long refreshTokenDaysTtl) {}
