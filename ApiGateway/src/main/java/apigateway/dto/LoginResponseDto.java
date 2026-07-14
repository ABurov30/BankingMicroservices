package apigateway.dto;

public record LoginResponseDto(
        String accessToken,
        String refreshToken,
        long accessTokenMinutesTtl,
        long refreshTokenDaysTtl
) {

}
