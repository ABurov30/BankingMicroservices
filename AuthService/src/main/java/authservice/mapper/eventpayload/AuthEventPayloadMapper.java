package authservice.mapper.eventpayload;

import java.util.Map;
import java.util.UUID;
import kafkacontracts.auth.*;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthEventPayloadMapper {
  default AuthUserCreatedEventPayload toAuthUserCreatedEventPayload(Map<String, Object> value) {
    return AuthUserCreatedEventPayload.newBuilder()
        .setAuthUserId(id(value))
        .setEmail(value.get("email").toString())
        .setFirstName(value.get("firstName").toString())
        .setLastName(value.get("lastName").toString())
        .setVerificationCode(value.get("verificationCode").toString())
        .build();
  }

  default AuthUserBlockedEventPayload toAuthUserBlockedEventPayload(Map<String, Object> value) {
    return AuthUserBlockedEventPayload.newBuilder()
        .setAuthUserId(id(value))
        .setEmail(value.get("email").toString())
        .build();
  }

  default AuthUserUnlockEventPayload toAuthUserUnlockEventPayload(Map<String, Object> value) {
    return AuthUserUnlockEventPayload.newBuilder()
        .setAuthUserId(id(value))
        .setEmail(value.get("email").toString())
        .build();
  }

  default AuthUserVerifiedEventPayload toAuthUserVerifiedEventPayload(Map<String, Object> value) {
    return AuthUserVerifiedEventPayload.newBuilder()
        .setAuthUserId(id(value))
        .setEmail(value.get("email").toString())
        .build();
  }

  default AuthUserRoleChangedEventPayload toAuthUserRoleChangedEventPayload(
      Map<String, Object> value) {
    return AuthUserRoleChangedEventPayload.newBuilder()
        .setAuthUserId(id(value))
        .setRole(value.get("role").toString())
        .build();
  }

  default AuthUserForgetPasswordEventPayload toAuthUserForgetPasswordEventPayload(
      Map<String, Object> value) {
    return AuthUserForgetPasswordEventPayload.newBuilder()
        .setAuthUserId(id(value))
        .setEmail(value.get("email").toString())
        .build();
  }

  private UUID id(Map<String, Object> value) {
    return UUID.fromString(value.get("authUserId").toString());
  }
}
