package apigateway.dto;

public record RefreshResponseDto(
        String accessToken,
        String refreshToken,
        long accessTokenMinutesTtl,
        long refreshTokenDaysTtl
) {
}
