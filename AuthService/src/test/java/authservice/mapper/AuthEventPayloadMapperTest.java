package authservice.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import authservice.mapper.eventpayload.AuthEventPayloadMapperImpl;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthEventPayloadMapperTest {
  @Test
  void mapsAllAuthEventPayloads() {
    UUID id = UUID.randomUUID();
    var common =
        Map.<String, Object>of(
            "authUserId", id,
            "email", "user@test",
            "firstName", "First",
            "lastName", "Last",
            "verificationCode", "123456",
            "status", "ACTIVE",
            "version", 1L,
            "role", "USER",
            "resetPasswordToken", "reset");
    var mapper = new AuthEventPayloadMapperImpl();
    assertThat(mapper.toAuthUserCreatedEventPayload(common).getAuthUserId()).isEqualTo(id);
    assertThat(mapper.toAuthUserStatusChangedEventPayload(common).getVersion()).isEqualTo(1L);
    assertThat(mapper.toAuthUserVerifiedEventPayload(common).getEmail()).isEqualTo("user@test");
    assertThat(mapper.toAuthUserRoleChangedEventPayload(common).getRole()).isEqualTo("USER");
    assertThat(mapper.toAuthUserForgetPasswordEventPayload(common).getResetPasswordToken())
        .isEqualTo("reset");
    assertThat(mapper.toAuthSocialAccountAuthUserCreatedEventPayload(common).getFirstName())
        .isEqualTo("First");
  }
}
