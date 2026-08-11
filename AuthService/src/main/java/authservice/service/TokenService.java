package authservice.service;

import authservice.config.JwtConfig;
import authservice.config.JwtProperties;
import authservice.entity.AuthUserEntity;
import enums.auth.Roles;
import authservice.repository.AuthUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class TokenService {
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private final JwtEncoder encoder;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(JwtConfig jwtConfig,
                        JwtProperties jwtProperties
                        ) {
        this.encoder = jwtConfig.jwtEncoder();
        this.jwtProperties = jwtProperties;
    }

    private JwtClaimsSet generateClaimsSet(AuthUserEntity userEntity, Roles roles) {
        Instant now = Instant.now();
        JwtClaimsSet jwtClaimsSet = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .audience(List.of(jwtProperties.audience()))
                .issuedAt(now)
                .id(UUID.randomUUID().toString())
                .subject(userEntity.getId().toString())
                .claim("status", userEntity.getStatus().name())
                .claim("roles", List.of(roles))
                .expiresAt(now.plus(jwtProperties.accessTokenTtlMinutes(), ChronoUnit.MINUTES))
                .build();
        return jwtClaimsSet;
    }

    public Jwt generateAccessToken(AuthUserEntity userEntity, Roles roles) {
        return encoder.encode(JwtEncoderParameters.from(generateClaimsSet(userEntity, roles)));
    }

    public String generateRefreshToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm is unavailable", e);
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public LocalDateTime refreshTokenExpiresAt() {
        return LocalDateTime.now().plusDays(jwtProperties.refreshTokenTtlDays());
    }

}
