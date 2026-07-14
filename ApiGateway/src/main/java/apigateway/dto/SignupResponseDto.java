package apigateway.dto;

public record SignupResponseDto(
        String accessToken,
        String refreshToken,
        long accessTokenMinutesTtl,
        long refreshTokenDaysTtl
) {}
