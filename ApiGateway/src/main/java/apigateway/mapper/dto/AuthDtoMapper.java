package apigateway.mapper.dto;

import apigateway.dto.auth.*;
import apigateway.dto.auth.SocialAccountResponse;
import apigateway.dto.user.GetAuthUserByIdResponseDto;
import auth.contract.v1.*;
import enums.auth.AuthUserStatus;
import enums.auth.Roles;
import enums.auth.SocialLoginProvider;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthDtoMapper {
  default VerifyAuthUserByCodeResponseDto toVerifyAuthUserByCodeResponseDto(
      VerifyAuthUserByCodeGrpcResponse value) {
    AuthTokenResponse tokens = value.getTokens();
    return new VerifyAuthUserByCodeResponseDto(
        tokens.getAccessToken(),
        tokens.getRefreshToken(),
        tokens.getAccessTokenMinutesTtl(),
        tokens.getRefreshTokenDaysTtl());
  }

  default LoginResponseDto toLoginResponseDto(LoginAuthGrpcResponse value) {
    AuthTokenResponse tokens = value.getTokens();
    return new LoginResponseDto(
        tokens.getAccessToken(),
        tokens.getRefreshToken(),
        tokens.getAccessTokenMinutesTtl(),
        tokens.getRefreshTokenDaysTtl());
  }

  default LoginResponseDto toLoginResponseDto(SocialLoginGrpcResponse value) {
    AuthTokenResponse tokens = value.getTokens();
    return new LoginResponseDto(
        tokens.getAccessToken(),
        tokens.getRefreshToken(),
        tokens.getAccessTokenMinutesTtl(),
        tokens.getRefreshTokenDaysTtl());
  }

  default RefreshResponseDto toRefreshResponseDto(RefreshAuthGrpcResponse value) {
    AuthTokenResponse tokens = value.getTokens();
    return new RefreshResponseDto(
        tokens.getAccessToken(),
        tokens.getRefreshToken(),
        tokens.getAccessTokenMinutesTtl(),
        tokens.getRefreshTokenDaysTtl());
  }

  default GetAuthUserByIdResponseDto toGetAuthUserByIdResponseDto(
      GetAuthUserByIdGrpcResponse value) {
    return new GetAuthUserByIdResponseDto(
        UUID.fromString(value.getAuthUserId()),
        value.getEmail(),
        Roles.valueOf(value.getRole()),
        AuthUserStatus.valueOf(value.getStatus()),
        value.getSocialAccountsList().stream().map(this::toSocialAccountResponse).toList());
  }

  private SocialAccountResponse toSocialAccountResponse(
      auth.contract.v1.SocialAccountResponse value) {
    return new SocialAccountResponse(
        SocialLoginProvider.valueOf(value.getProvider()), value.getEmail());
  }

  default ResetPasswordsResponseDto toResetPasswordsResponseDto(ResetPasswordsGrpcResponse value) {
    AuthTokenResponse tokens = value.getTokens();
    return new ResetPasswordsResponseDto(
        tokens.getAccessToken(),
        tokens.getRefreshToken(),
        tokens.getAccessTokenMinutesTtl(),
        tokens.getRefreshTokenDaysTtl());
  }

  default ChangePasswordResponseDto toChangePasswordResponseDto(
      ChangePasswordGrpcResponse response) {
    return new ChangePasswordResponseDto(
        response.getRefreshToken(), response.getRefreshTokenDaysTtl());
  }
}
