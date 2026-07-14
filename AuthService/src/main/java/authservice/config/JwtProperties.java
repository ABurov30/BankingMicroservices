package authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;


@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String issuer,
        String audience,
        long accessTokenTtlMinutes,
        long refreshTokenTtlDays,
        Resource privateKeyLocation,
        Resource publicKeyLocation
) {
}
