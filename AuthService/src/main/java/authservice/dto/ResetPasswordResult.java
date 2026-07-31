package authservice.dto;

public record ResetPasswordResult(
        String accessToken,
        String refreshToken,
        long accessTokenMinutesTtl,
        long refreshTokenDaysTtl
) {
}
