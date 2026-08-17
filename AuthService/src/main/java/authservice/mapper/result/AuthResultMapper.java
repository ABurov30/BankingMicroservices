package authservice.mapper.result;

import authservice.dto.GetAuthUserByIdResult;
import authservice.dto.LoginResult;
import authservice.dto.RefreshResult;
import authservice.dto.ResetPasswordResult;
import authservice.dto.SocialLoginResult;
import authservice.dto.TokenPair;
import authservice.dto.VerifyAuthUserByCodeResult;
import authservice.entity.AuthUserEntity;
import authservice.entity.UserRoleEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthResultMapper {
  default VerifyAuthUserByCodeResult toVerifyAuthUserByCodeResult(TokenPair tokenPair) {
    return new VerifyAuthUserByCodeResult(
        tokenPair.accessToken(),
        tokenPair.refreshToken(),
        tokenPair.accessTokenMinutesTtl(),
        tokenPair.refreshTokenDaysTtl());
  }

  default LoginResult toLoginResult(TokenPair tokenPair) {
    return new LoginResult(
        tokenPair.accessToken(),
        tokenPair.refreshToken(),
        tokenPair.accessTokenMinutesTtl(),
        tokenPair.refreshTokenDaysTtl());
  }

  default RefreshResult toRefreshResult(TokenPair tokenPair) {
    return new RefreshResult(
        tokenPair.accessToken(),
        tokenPair.refreshToken(),
        tokenPair.accessTokenMinutesTtl(),
        tokenPair.refreshTokenDaysTtl());
  }

  default GetAuthUserByIdResult toGetAuthUserByIdResult(
      AuthUserEntity authUser, UserRoleEntity userRole) {
    return new GetAuthUserByIdResult(
        authUser.getId(), authUser.getStatus(), authUser.getEmail(), userRole.getRole().getName());
  }

  default ResetPasswordResult toResetPasswordResult(TokenPair tokenPair) {
    return new ResetPasswordResult(
        tokenPair.accessToken(),
        tokenPair.refreshToken(),
        tokenPair.accessTokenMinutesTtl(),
        tokenPair.refreshTokenDaysTtl());
  }

  default SocialLoginResult toSocialLoginResult(TokenPair tokenPair) {
    return new SocialLoginResult(
        tokenPair.accessToken(),
        tokenPair.refreshToken(),
        tokenPair.accessTokenMinutesTtl(),
        tokenPair.refreshTokenDaysTtl());
  }
}
