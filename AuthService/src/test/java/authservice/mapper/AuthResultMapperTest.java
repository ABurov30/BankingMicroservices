package authservice.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import authservice.dto.TokenPair;
import authservice.entity.AuthSocialAccountsEntity;
import authservice.entity.AuthUserEntity;
import authservice.entity.RoleEntity;
import authservice.entity.UserRoleEntity;
import authservice.mapper.result.AuthResultMapperImpl;
import enums.auth.Roles;
import enums.auth.SocialLoginProvider;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthResultMapperTest {
  @Test
  void mapsTokenResultsUserAndSocialAccounts() {
    var mapper = new AuthResultMapperImpl();
    var tokens = new TokenPair("access", "refresh", 10, 20);
    assertThat(mapper.toLoginResult(tokens).accessToken()).isEqualTo("access");
    assertThat(mapper.toRefreshResult(tokens).refreshToken()).isEqualTo("refresh");
    assertThat(mapper.toVerifyAuthUserByCodeResult(tokens).refreshTokenDaysTtl()).isEqualTo(20);
    assertThat(mapper.toResetPasswordResult(tokens).accessToken()).isEqualTo("access");
    assertThat(mapper.toSocialLoginResult(tokens).accessToken()).isEqualTo("access");
    assertThat(mapper.toChangePasswordResult("new", 30).refreshToken()).isEqualTo("new");

    var user = new AuthUserEntity();
    user.setId(UUID.randomUUID());
    user.setEmail("user@test");
    var role = new RoleEntity();
    role.setName(Roles.USER);
    var relation = new UserRoleEntity();
    relation.setAuthUser(user);
    relation.setRole(role);
    var social = new AuthSocialAccountsEntity();
    social.setProvider(SocialLoginProvider.GOOGLE);
    social.setProviderEmail("social@test");
    assertThat(mapper.toGetAuthUserByIdResult(user, relation, List.of(social)).socialAccounts())
        .singleElement()
        .satisfies(value -> assertThat(value.email()).isEqualTo("social@test"));
  }
}
