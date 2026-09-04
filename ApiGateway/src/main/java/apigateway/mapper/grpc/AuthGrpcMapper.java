package apigateway.mapper.grpc;

import apigateway.dto.auth.*;
import apigateway.dto.user.GetRoleByAuthUserIdRequestDto;
import auth.contract.v1.*;
import java.util.Locale;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthGrpcMapper {
  default SignupAuthGrpcRequest toSignupAuthGrpcRequest(SignupRequestDto value) {
    return SignupAuthGrpcRequest.newBuilder()
        .setEmail(value.email().trim().toLowerCase(Locale.ROOT))
        .setPassword(value.password())
        .setFirstName(value.firstName())
        .setLastName(value.lastName())
        .build();
  }

  default LoginAuthGrpcRequest toLoginAuthGrpcRequest(LoginRequestDto value) {
    return LoginAuthGrpcRequest.newBuilder()
        .setEmail(value.email().trim().toLowerCase(Locale.ROOT))
        .setPassword(value.password())
        .build();
  }

  default LogoutAuthGrpcRequest toLogoutAuthGrpcRequest(LogoutRequestDto value) {
    return LogoutAuthGrpcRequest.newBuilder().setRefreshToken(value.refreshToken()).build();
  }

  default RefreshAuthGrpcRequest toRefreshAuthGrpcRequest(RefreshRequestDto value) {
    return RefreshAuthGrpcRequest.newBuilder().setRefreshToken(value.refreshToken()).build();
  }

  default ChangePasswordGrpcRequest toChangePasswordGrpcRequest(
      ChangePasswordRequestDto value, UUID authUserId) {
    return ChangePasswordGrpcRequest.newBuilder()
        .setAuthUserId(authUserId.toString())
        .setNewPassword(value.newPassword())
        .setOldPassword(value.oldPassword())
        .build();
  }

  default BlockAuthGrpcRequest toBlockUserGrpcRequest(BlockAuthUserRequestDto value) {
    return BlockAuthGrpcRequest.newBuilder().setAuthUserId(value.authUserId().toString()).build();
  }

  default UnlockAuthGrpcRequest toUnlockUserGrpcRequest(UnlockAuthUserRequestDto value) {
    return UnlockAuthGrpcRequest.newBuilder().setAuthUserId(value.authUserId().toString()).build();
  }

  default ChangeAuthUserRoleGrpcRequest toChangeAuthUserRoleGrpcRequest(
      ChangeAuthUserRoleRequestDto value) {
    return ChangeAuthUserRoleGrpcRequest.newBuilder()
        .setAuthUserId(value.authUserId().toString())
        .setRole(value.role().name())
        .build();
  }

  default VerifyAuthUserByCodeGrpcRequest toVerifyAuthUserByCodeGrpcRequest(
      VerifyAuthUserByCodeRequestDto value) {
    return VerifyAuthUserByCodeGrpcRequest.newBuilder()
        .setAuthUserId(value.authUserId().toString())
        .setVerificationCode(value.verificationCode())
        .build();
  }

  default VerifyAuthUserByPrivilegeRoleGrpcRequest toVerifyAuthUserByPrivilegeRoleGrpcRequest(
      VerifyAuthUserByPrivilegeRoleRequestDto value) {
    return VerifyAuthUserByPrivilegeRoleGrpcRequest.newBuilder()
        .setAuthUserId(value.authUserId().toString())
        .setRole(value.roles().name())
        .build();
  }

  default GetAuthUserByIdGrpcRequest toGetAuthUserByIdGrpcRequest(
      GetRoleByAuthUserIdRequestDto value) {
    return GetAuthUserByIdGrpcRequest.newBuilder()
        .setAuthUserId(value.authUserId().toString())
        .build();
  }

  default ForgetPasswordGrpcRequest toForgetPasswordGrpcRequest(ForgetPasswordRequestDto value) {
    return ForgetPasswordGrpcRequest.newBuilder().setEmail(value.email()).build();
  }

  default ResetPasswordGrpcRequest toResetPasswordGrpcRequest(ResetPasswordRequestDto value) {
    return ResetPasswordGrpcRequest.newBuilder()
        .setAuthUserId(value.authUserId().toString())
        .setNewPassword(value.newPassword())
        .build();
  }

  default SocialLoginGrpcRequest toSocialLoginGrpcRequest(SocialLoginRequestDto value) {
    return SocialLoginGrpcRequest.newBuilder()
        .setProvider(value.provider().name())
        .setProviderUserId(value.providerUserId())
        .setEmail(value.email().trim().toLowerCase(Locale.ROOT))
        .setIsEmailVerified(Boolean.TRUE.equals(value.isEmailVerified()))
        .setFirstName(value.firstName() == null ? "" : value.firstName())
        .setLastName(value.lastName() == null ? "" : value.lastName())
        .build();
  }
}
