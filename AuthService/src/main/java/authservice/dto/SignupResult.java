package authservice.dto;

public record SignupResult(
        String accessToken,
        String refreshToken,
        long accessTokenMinutesTtl,
        long refreshTokenDaysTtl
) {
}
