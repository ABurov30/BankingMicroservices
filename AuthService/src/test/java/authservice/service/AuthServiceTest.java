package authservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import authservice.config.JwtProperties;
import authservice.dto.*;
import authservice.entity.*;
import authservice.exception.*;
import authservice.mapper.result.AuthResultMapper;
import authservice.repository.*;
import enums.auth.AuthUserStatus;
import enums.auth.Roles;
import enums.auth.SocialLoginProvider;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kafkacontracts.auth.AuthEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

class AuthServiceTest {
  private final AuthUserRepository users = mock(AuthUserRepository.class);
  private final UserRoleRepository userRoles = mock(UserRoleRepository.class);
  private final RoleRepository roles = mock(RoleRepository.class);
  private final PasswordEncoder passwords = mock(PasswordEncoder.class);
  private final TokenService tokens = mock(TokenService.class);
  private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
  private final JwtProperties properties =
      new JwtProperties("issuer", "audience", 15, 30, 10, null, null);
  private final AuthOutboxEventRepository outbox = mock(AuthOutboxEventRepository.class);
  private final AuthSocialAccountsRepository social = mock(AuthSocialAccountsRepository.class);
  private final AuthResultMapper resultMapper = mock(AuthResultMapper.class);
  private final ResetPasswordTokenRepository resetTokens = mock(ResetPasswordTokenRepository.class);
  private final AuthService service =
      new AuthService(
          users,
          userRoles,
          roles,
          passwords,
          tokens,
          refreshTokens,
          properties,
          outbox,
          social,
          resultMapper,
          resetTokens);
  private final UUID userId = UUID.randomUUID();
  private AuthUserEntity user;
  private RoleEntity userRole;

  @BeforeEach
  void setUp() {
    user = new AuthUserEntity();
    user.setId(userId);
    user.setEmail("user@test");
    user.setStatus(AuthUserStatus.ACTIVE);
    userRole = new RoleEntity();
    userRole.setName(Roles.USER);
    when(tokens.generateRefreshToken()).thenReturn("refresh");
    when(tokens.hashToken("refresh")).thenReturn("refresh-hash");
    when(tokens.refreshTokenExpiresAt()).thenReturn(LocalDateTime.now().plusDays(1));
    Jwt jwt = mock(Jwt.class);
    when(jwt.getTokenValue()).thenReturn("access");
    when(tokens.generateAccessToken(any(), any())).thenReturn(jwt);
  }

  @Test
  void signupCreatesUserRoleTokensAndOutbox() {
    when(users.existsByEmail("new@test")).thenReturn(false);
    when(passwords.encode(anyString())).thenReturn("encoded");
    when(users.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              var value = i.getArgument(0, AuthUserEntity.class);
              value.setId(userId);
              return value;
            });
    when(roles.findByName(Roles.USER)).thenReturn(Optional.of(userRole));
    when(userRoles.save(any(UserRoleEntity.class)))
        .thenAnswer(i -> i.getArgument(0, UserRoleEntity.class));
    var expected = new VerifyAuthUserByCodeResult("access", "refresh", 15, 30);
    when(resultMapper.toVerifyAuthUserByCodeResult(any())).thenReturn(expected);

    var result = service.signup(new SignupCommand("new@test", "password", "First", "Last"));

    assertThat(result).isSameAs(expected);
    verify(userRoles).save(any(UserRoleEntity.class));
    verify(outbox)
        .save(
            argThat(event -> event.getEventType().equals(AuthEventType.AUTH_USER_CREATED.name())));
    verify(refreshTokens).save(any(RefreshTokenEntity.class));
  }

  @Test
  void loginRejectsBadCredentialsAndInactiveUsers() {
    when(users.findByEmail("user@test")).thenReturn(Optional.of(user));
    when(passwords.matches("bad", null)).thenReturn(false);
    assertThatThrownBy(() -> service.login(new LoginCommand("user@test", "bad")))
        .isInstanceOf(InvalidEmailOrPasswordException.class);
    when(passwords.matches("good", null)).thenReturn(true);
    user.setStatus(AuthUserStatus.BLOCKED);
    assertThatThrownBy(() -> service.login(new LoginCommand("user@test", "good")))
        .isInstanceOf(AuthUserNotActiveException.class);
  }

  @Test
  void loginSuccessLogoutAndRefreshRotateTokens() {
    when(users.findByEmail("user@test")).thenReturn(Optional.of(user));
    when(passwords.matches("good", null)).thenReturn(true);
    var relation = new UserRoleEntity();
    relation.setRole(userRole);
    when(userRoles.findByAuthUserId(userId)).thenReturn(Optional.of(relation));
    when(roles.findByName(Roles.USER)).thenReturn(Optional.of(userRole));
    when(resultMapper.toLoginResult(any())).thenReturn(null);
    assertThat(service.login(new LoginCommand("user@test", "good"))).isNull();

    var refresh = new RefreshTokenEntity();
    refresh.setTokenHash("refresh-hash");
    refresh.setAuthUser(user);
    refresh.setExpiresAt(LocalDateTime.now().plusHours(1));
    when(tokens.hashToken("refresh")).thenReturn("refresh-hash");
    when(refreshTokens.findByTokenHash("refresh-hash")).thenReturn(Optional.of(refresh));
    service.logout(new LogoutCommand("refresh"));
    assertThat(refresh.getRevokedAt()).isNotNull();
    refresh.setRevokedAt(null);
    when(refreshTokens.findByTokenHashForUpdate("refresh-hash")).thenReturn(Optional.of(refresh));
    when(resultMapper.toRefreshResult(any())).thenReturn(null);
    assertThat(service.refresh(new RefreshCommand("refresh"))).isNull();
    verify(refreshTokens, times(2)).save(refresh);
  }

  @Test
  void changePasswordRevokesOldTokensAndCreatesNewOne() {
    when(users.findById(userId)).thenReturn(Optional.of(user));
    when(passwords.matches("old", null)).thenReturn(true);
    when(passwords.encode("new")).thenReturn("new-hash");
    when(resultMapper.toChangePasswordResult("refresh", 30)).thenReturn(null);
    var old = new RefreshTokenEntity();
    when(refreshTokens.findAllByAuthUserId(userId)).thenReturn(List.of(old));
    service.changePassword(new ChangePasswordCommand(userId, "old", "new"));
    assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    assertThat(old.getRevokedAt()).isNotNull();
    verify(refreshTokens).saveAll(List.of(old));
  }

  @Test
  void blockUnlockAndRoleChangePublishEvents() {
    when(users.findById(userId)).thenReturn(Optional.of(user));
    service.blockUser(new BlockAuthUserCommand(userId));
    assertThat(user.getStatus()).isEqualTo(AuthUserStatus.BLOCKED);
    service.unlockUser(new UnlockAuthUserCommand(userId));
    assertThat(user.getStatus()).isEqualTo(AuthUserStatus.ACTIVE);
    var manager = new RoleEntity();
    manager.setName(Roles.MANAGER);
    var relation = new UserRoleEntity();
    relation.setAuthUser(user);
    relation.setRole(userRole);
    when(userRoles.findByAuthUserId(userId)).thenReturn(Optional.of(relation));
    when(roles.findByName(Roles.MANAGER)).thenReturn(Optional.of(manager));
    service.changeAuthUserRole(new ChangeAuthUserRoleCommand(userId, Roles.MANAGER));
    assertThat(relation.getRole()).isSameAs(manager);
    verify(outbox, atLeast(3)).save(any());
  }

  @Test
  void verifyByCodeActivatesUserAndIssuesTokens() {
    user.setStatus(AuthUserStatus.PENDING);
    user.setVerificationCodeHash("code-hash");
    when(users.findById(userId)).thenReturn(Optional.of(user));
    when(passwords.matches("123", "code-hash")).thenReturn(true);
    var relation = new UserRoleEntity();
    relation.setRole(userRole);
    when(userRoles.findByAuthUserId(userId)).thenReturn(Optional.of(relation));
    when(resultMapper.toVerifyAuthUserByCodeResult(any())).thenReturn(null);
    service.verifyByCode(new VerifyAuthUserByCodeCommand(userId, "123"));
    assertThat(user.isEmailVerified()).isTrue();
    assertThat(user.getStatus()).isEqualTo(AuthUserStatus.ACTIVE);
    verify(outbox)
        .save(
            argThat(event -> event.getEventType().equals(AuthEventType.AUTH_USER_VERIFIED.name())));
  }

  @Test
  void privilegedVerificationRequiresManagerOrAdmin() {
    assertThatThrownBy(
            () ->
                service.verifyByPrivilegedRole(
                    new VerifyAuthUserByPrivilegeRoleCommand(userId, Roles.USER)))
        .isInstanceOf(VerificationByRoleNotAllowedException.class);
    when(users.findById(userId)).thenReturn(Optional.of(user));
    user.setEmailVerified(false);
    service.verifyByPrivilegedRole(new VerifyAuthUserByPrivilegeRoleCommand(userId, Roles.ADMIN));
    assertThat(user.isEmailVerified()).isTrue();
  }

  @Test
  void socialLoginHandlesExistingSocialEmailAndNewUsers() {
    var command =
        new SocialLoginCommand(
            SocialLoginProvider.GOOGLE, "provider-id", "user@test", true, "First", "Last");
    var relation = new UserRoleEntity();
    relation.setRole(userRole);
    relation.setAuthUser(user);
    var linked = new AuthSocialAccountsEntity();
    linked.setAuthUser(user);
    when(social.findByProviderAndProviderUserId(SocialLoginProvider.GOOGLE, "provider-id"))
        .thenReturn(Optional.of(linked), Optional.empty(), Optional.empty());
    when(userRoles.findByAuthUserId(userId)).thenReturn(Optional.of(relation));
    when(resultMapper.toSocialLoginResult(any())).thenReturn(null);
    when(roles.findByName(Roles.USER)).thenReturn(Optional.of(userRole));
    assertThat(service.socialLogin(command)).isNull();

    when(users.findByEmail("user@test")).thenReturn(Optional.of(user));
    assertThat(service.socialLogin(command)).isNull();
    verify(social).save(any(AuthSocialAccountsEntity.class));

    when(users.findByEmail("user@test")).thenReturn(Optional.empty());
    when(users.save(any(AuthUserEntity.class)))
        .thenAnswer(
            i -> {
              var saved = i.getArgument(0, AuthUserEntity.class);
              saved.setId(userId);
              return saved;
            });
    when(userRoles.save(any(UserRoleEntity.class)))
        .thenAnswer(i -> i.getArgument(0, UserRoleEntity.class));
    assertThat(service.socialLogin(command)).isNull();
    verify(outbox)
        .save(
            argThat(
                event ->
                    event
                        .getEventType()
                        .equals(AuthEventType.AUTH_SOCIAL_ACCOUNT_AUTH_USER_CREATED.name())));
  }

  @Test
  void batchReadRejectsMissingUsersAndReturnsEmptyForEmptyInput() {
    assertThat(service.getAuthUserByIds(new GetAuthUserByIdsCommand(List.of()))).isEmpty();
    when(users.findByIdIn(any())).thenReturn(List.of());
    assertThatThrownBy(() -> service.getAuthUserByIds(new GetAuthUserByIdsCommand(List.of(userId))))
        .isInstanceOf(AuthUserNotFoundException.class);
  }

  @Test
  void rejectsDuplicateSignupRevokedAndExpiredRefreshTokens() {
    when(users.existsByEmail("user@test")).thenReturn(true);
    assertThatThrownBy(() -> service.signup(new SignupCommand("user@test", "p", "F", "L")))
        .isInstanceOf(EmailAlreadyExistsException.class);
    var token = new RefreshTokenEntity();
    token.setRevokedAt(LocalDateTime.now());
    token.setExpiresAt(LocalDateTime.now().plusHours(1));
    when(refreshTokens.findByTokenHash("refresh-hash")).thenReturn(Optional.of(token));
    assertThatThrownBy(() -> service.logout(new LogoutCommand("refresh")))
        .isInstanceOf(RefreshTokenAlreadyRevokedException.class);
    token.setRevokedAt(null);
    token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
    assertThatThrownBy(() -> service.logout(new LogoutCommand("refresh")))
        .isInstanceOf(RefreshTokenAlreadyExpiredException.class);
  }

  @Test
  void mapsConcurrentSignupConstraintToDuplicateEmail() {
    when(users.existsByEmail("new@test")).thenReturn(false);
    when(passwords.encode(anyString())).thenReturn("encoded");
    when(users.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
    assertThatThrownBy(() -> service.signup(new SignupCommand("new@test", "p", "F", "L")))
        .isInstanceOf(EmailAlreadyExistsException.class);
  }
}
