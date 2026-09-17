package authservice.mapper.eventpayload;

import java.util.Map;
import java.util.UUID;
import kafkacontracts.auth.*;
import kafkacontracts.cache.CacheInvalidationEventPayload;
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
        .setStatus(value.get("status").toString())
        .setRole(value.get("role").toString())
        .build();
  }

  default AuthUserStatusChangedEventPayload toAuthUserStatusChangedEventPayload(
      Map<String, Object> value) {
    return AuthUserStatusChangedEventPayload.newBuilder()
        .setAuthUserId(id(value))
        .setStatus(value.get("status").toString())
        .setVersion(Long.parseLong(value.get("version").toString()))
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
        .setResetPasswordToken(value.get("resetPasswordToken").toString())
        .setEmail(value.get("email").toString())
        .setAuthUserId(id(value))
        .build();
  }

  default AuthSocialAccountAuthUserCreatedEventPayload
      toAuthSocialAccountAuthUserCreatedEventPayload(Map<String, Object> value) {
    return AuthSocialAccountAuthUserCreatedEventPayload.newBuilder()
        .setAuthUserId(id(value))
        .setEmail(value.get("email").toString())
        .setFirstName(value.get("firstName").toString())
        .setLastName(value.get("lastName").toString())
        .build();
  }

  default CacheInvalidationEventPayload toCacheInvalidationEventPayload(
      Map<String, Object> payload) {
    Object keys = payload.get("keys");
    if (!(keys instanceof Iterable<?> iterable)) {
      throw new IllegalArgumentException("Outbox payload field 'keys' is required");
    }

    var values = new java.util.ArrayList<String>();
    iterable.forEach(value -> values.add(value.toString()));
    return CacheInvalidationEventPayload.newBuilder().setKeys(values).build();
  }

  private UUID id(Map<String, Object> value) {
    return UUID.fromString(value.get("authUserId").toString());
  }
}
