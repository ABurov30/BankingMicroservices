package authservice.mapper.grpc;

import auth.contract.v1.*;
import authservice.dto.*;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthGrpcMapper {
  default LoginAuthGrpcResponse toLoginGrpcResponse(LoginResult result) {
    return LoginAuthGrpcResponse.newBuilder()
        .setTokens(
            tokens(
                result.accessToken(),
                result.refreshToken(),
                result.accessTokenMinutesTtl(),
                result.refreshTokenDaysTtl()))
        .build();
  }

  default RefreshAuthGrpcResponse toRefreshGrpcResponse(RefreshResult result) {
    return RefreshAuthGrpcResponse.newBuilder()
        .setTokens(
            tokens(
                result.accessToken(),
                result.refreshToken(),
                result.accessTokenMinutesTtl(),
                result.refreshTokenDaysTtl()))
        .build();
  }

  default VerifyAuthUserByCodeGrpcResponse toVerifyAuthUserByCodeGrpcResponse(
      VerifyAuthUserByCodeResult result) {
    return VerifyAuthUserByCodeGrpcResponse.newBuilder()
        .setTokens(
            tokens(
                result.accessToken(),
                result.refreshToken(),
                result.accessTokenMinutesTtl(),
                result.refreshTokenDaysTtl()))
        .build();
  }

  default GetAuthUserByIdGrpcResponse toGetAuthUserByIdGrpcResponse(GetAuthUserByIdResult result) {
    return GetAuthUserByIdGrpcResponse.newBuilder()
        .setAuthUserId(result.authUserId().toString())
        .setStatus(result.status().name())
        .setEmail(result.email())
        .setRole(result.role().name())
        .build();
  }

  default ResetPasswordsGrpcResponse toResetPasswordsGrpcResponse(ResetPasswordResult result) {
    return ResetPasswordsGrpcResponse.newBuilder()
        .setTokens(
            tokens(
                result.accessToken(),
                result.refreshToken(),
                result.accessTokenMinutesTtl(),
                result.refreshTokenDaysTtl()))
        .build();
  }

  default SocialLoginGrpcResponse toSocialLoginGrpcResponse(SocialLoginResult result) {
    return SocialLoginGrpcResponse.newBuilder()
        .setTokens(
            tokens(
                result.accessToken(),
                result.refreshToken(),
                result.accessTokenMinutesTtl(),
                result.refreshTokenDaysTtl()))
        .build();
  }

  private AuthTokenResponse tokens(
      String accessToken, String refreshToken, long accessTtl, long refreshTtl) {
    return AuthTokenResponse.newBuilder()
        .setRefreshToken(refreshToken)
        .setAccessToken(accessToken)
        .setAccessTokenMinutesTtl((int) accessTtl)
        .setRefreshTokenDaysTtl((int) refreshTtl)
        .build();
  }
}
