package authservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import authservice.config.JwtConfig;
import authservice.config.JwtProperties;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class TokenServiceTest {
  @Test
  void hashesTokensGeneratesRefreshValuesAndCalculatesExpirations() {
    var properties = new JwtProperties("issuer", "audience", 15, 30, 10, null, null);
    var config = mock(JwtConfig.class);
    when(config.jwtEncoder()).thenReturn(mock(JwtEncoder.class));
    var service = new TokenService(config, properties);
    assertThat(service.hashToken("value")).isNotEqualTo("value").hasSize(44);
    assertThat(service.generateRefreshToken()).isNotBlank().hasSizeGreaterThan(40);
    LocalDateTime refresh = service.refreshTokenExpiresAt();
    LocalDateTime reset = service.resetPasswordTokenExpiresAt();
    assertThat(refresh).isAfter(LocalDateTime.now().plusDays(29));
    assertThat(reset).isAfter(LocalDateTime.now().plusMinutes(9));
  }
}
