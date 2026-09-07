package apigateway.dto.response.auth;

import enums.auth.SocialLoginProvider;

public record SocialAccountResponse(SocialLoginProvider provider, String email) {}
