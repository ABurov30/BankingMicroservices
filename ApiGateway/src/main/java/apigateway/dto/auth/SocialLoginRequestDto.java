package apigateway.dto.auth;

import enums.auth.SocialLoginProvider;

public record SocialLoginRequestDto(
    SocialLoginProvider provider,
    String providerUserId,
    String email,
    Boolean isEmailVerified,
    String firstName,
    String lastName) {}
