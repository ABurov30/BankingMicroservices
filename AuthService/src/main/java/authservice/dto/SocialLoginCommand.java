package authservice.dto;

import enums.auth.SocialLoginProvider;

public record SocialLoginCommand(
    SocialLoginProvider provider,
    String providerUserId,
    String email,
    Boolean isEmailVerified,
    String firstName,
    String lastName) {}
