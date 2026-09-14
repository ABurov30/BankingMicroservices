package authservice.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import authservice.dto.GetAuthUserByIdsCommand;
import authservice.entity.*;
import authservice.exception.*;
import authservice.mapper.result.AuthResultMapper;
import authservice.repository.*;
import enums.auth.Roles;
import enums.auth.SocialLoginProvider;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthBatchReadTest {
  @Mock private AuthUserRepository users;
  @Mock private UserRoleRepository roles;
  @Mock private AuthSocialAccountsRepository socials;
  @Spy private AuthResultMapper mapper = new AuthResultMapper() {};
  @InjectMocks private AuthService service;

  @Test
  void emptyBatchSkipsDatabase() {
    assertThat(service.getAuthUserByIds(new GetAuthUserByIdsCommand(List.of()))).isEmpty();
    verifyNoInteractions(users, roles, socials);
  }

  @Test
  void batchesAllDataAndMatchesByIdRegardlessOfDatabaseOrder() {
    var first = user();
    var second = user();
    var ids = List.of(first.getId(), second.getId());
    var social = new AuthSocialAccountsEntity();
    social.setAuthUser(second);
    social.setProvider(SocialLoginProvider.GOOGLE);
    social.setProviderEmail("social@example.com");
    when(users.findByIdIn(ids)).thenReturn(List.of(second, first));
    when(roles.findByAuthUserIdIn(ids))
        .thenReturn(List.of(role(second, Roles.ADMIN), role(first, Roles.USER)));
    when(socials.findAllByAuthUserIdIn(ids)).thenReturn(List.of(social));
    var results =
        service.getAuthUserByIds(
            new GetAuthUserByIdsCommand(List.of(first.getId(), second.getId(), first.getId())));
    assertThat(results)
        .extracting(value -> value.authUserId())
        .containsExactly(first.getId(), second.getId());
    assertThat(results.get(0).role()).isEqualTo(Roles.USER);
    assertThat(results.get(0).socialAccounts()).isEmpty();
    assertThat(results.get(1).role()).isEqualTo(Roles.ADMIN);
    assertThat(results.get(1).socialAccounts()).hasSize(1);
    verify(users).findByIdIn(ids);
    verify(roles).findByAuthUserIdIn(ids);
    verify(socials).findAllByAuthUserIdIn(ids);
    verifyNoMoreInteractions(users, roles, socials);
  }

  @Test
  void missingUserFailsInsteadOfReturningPartialResults() {
    var ids = List.of(UUID.randomUUID());
    when(users.findByIdIn(ids)).thenReturn(List.of());
    assertThatThrownBy(() -> service.getAuthUserByIds(new GetAuthUserByIdsCommand(ids)))
        .isInstanceOf(AuthUserNotFoundException.class);
    verifyNoInteractions(roles, socials);
  }

  @Test
  void missingRoleFailsInsteadOfReturningPartialResults() {
    var user = user();
    var ids = List.of(user.getId());
    when(users.findByIdIn(ids)).thenReturn(List.of(user));
    when(roles.findByAuthUserIdIn(ids)).thenReturn(List.of());
    assertThatThrownBy(() -> service.getAuthUserByIds(new GetAuthUserByIdsCommand(ids)))
        .isInstanceOf(RoleNotFoundException.class);
    verifyNoInteractions(socials);
  }

  private AuthUserEntity user() {
    var user = new AuthUserEntity();
    user.setId(UUID.randomUUID());
    return user;
  }

  private UserRoleEntity role(AuthUserEntity user, Roles name) {
    var role = new RoleEntity();
    role.setName(name);
    var userRole = new UserRoleEntity();
    userRole.setAuthUser(user);
    userRole.setRole(role);
    return userRole;
  }
}
