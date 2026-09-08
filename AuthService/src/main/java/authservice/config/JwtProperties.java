package authservice.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String issuer,
    String audience,
    @Positive long accessTokenTtlMinutes,
    @Positive long refreshTokenTtlDays,
    @Positive long resetPasswordTokenTtlMinutes,
    Resource privateKeyLocation,
    Resource publicKeyLocation) {}
