package apigateway.dto.auth;

public record RefreshResponseDto(
        String accessToken,
        String refreshToken,
        long accessTokenMinutesTtl,
        long refreshTokenDaysTtl
) {
}
