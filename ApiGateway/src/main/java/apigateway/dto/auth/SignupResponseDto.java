package apigateway.dto.auth;

public record SignupResponseDto(
        String accessToken,
        String refreshToken,
        long accessTokenMinutesTtl,
        long refreshTokenDaysTtl
) {}
