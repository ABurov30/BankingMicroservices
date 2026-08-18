package apigateway.dto.auth;

import enums.auth.SocialLoginProvider;

public record SocialAccountResponse(SocialLoginProvider provider, String email) {}
