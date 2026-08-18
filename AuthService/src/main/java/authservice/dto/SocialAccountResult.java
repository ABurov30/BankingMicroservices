package authservice.dto;

import enums.auth.SocialLoginProvider;

public record SocialAccountResult(SocialLoginProvider provider, String email) {}
