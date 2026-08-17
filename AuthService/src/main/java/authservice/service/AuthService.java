package authservice.service;

import authservice.config.JwtProperties;
import authservice.dto.*;
import authservice.entity.*;
import authservice.exception.*;
import authservice.mapper.result.AuthResultMapper;
import authservice.repository.*;
import enums.auth.AuthUserStatus;
import enums.auth.Roles;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kafkacontracts.auth.AuthEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private static final Logger log = LoggerFactory.getLogger(AuthService.class);
  private final AuthUserRepository authUserRepository;
  private final UserRoleRepository userRoleRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtProperties jwtProperties;
  private final AuthOutboxEventRepository authOutboxEventRepository;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private final AuthSocialAccountsRepository authSocialAccountsRepository;
  private final AuthResultMapper authResultMapper;

  public AuthService(
      AuthUserRepository authUserRepository,
      UserRoleRepository userRoleRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService,
      RoleRepository roleRepository,
      RefreshTokenRepository refreshTokenRepository,
      JwtProperties jwtProperties,
      AuthOutboxEventRepository authOutboxEventRepository,
      AuthSocialAccountsRepository authSocialAccountsRepository,
      AuthResultMapper authResultMapper) {
    this.authUserRepository = authUserRepository;
    this.userRoleRepository = userRoleRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.roleRepository = roleRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.jwtProperties = jwtProperties;
    this.authOutboxEventRepository = authOutboxEventRepository;
    this.authSocialAccountsRepository = authSocialAccountsRepository;
    this.authResultMapper = authResultMapper;
  }

  private String generateVerificationCode() {
    int code = SECURE_RANDOM.nextInt(1_000_000);
    return String.format("%06d", code);
  }

  @Transactional
  public VerifyAuthUserByCodeResult signup(SignupCommand signupCommand) {
    if (authUserRepository.existsByEmail(signupCommand.email())) {
      throw new EmailAlreadyExistsException(signupCommand.email());
    }

    AuthUserEntity userEntity = new AuthUserEntity();
    userEntity.setEmail(signupCommand.email());
    String verificationCode = generateVerificationCode();
    userEntity.setVerificationCodeHash(passwordEncoder.encode(verificationCode));
    userEntity.setPasswordHash(passwordEncoder.encode(signupCommand.password()));

    AuthUserEntity savedUser;
    try {
      savedUser = authUserRepository.saveAndFlush(userEntity);
    } catch (DataIntegrityViolationException e) {
      log.warn("Sign-up rejected because the email is already registered", e);
      throw new EmailAlreadyExistsException(signupCommand.email());
    }

    RoleEntity roleEntity =
        roleRepository
            .findByName(Roles.USER)
            .orElseThrow(() -> new RoleNotFoundException(savedUser.getId()));

    UserRoleEntity userRoleEntity = new UserRoleEntity();
    userRoleEntity.setAuthUser(savedUser);
    userRoleEntity.setRole(roleEntity);
    UserRoleEntity savedUserRole = userRoleRepository.save(userRoleEntity);

    TokenPair tokenPair = issueTokenPair(savedUser, savedUserRole.getRole().getName());

    saveAuthOutboxEvent(
        savedUser.getId(),
        AuthEventType.AUTH_USER_CREATED,
        Map.of(
            "authUserId", savedUser.getId(),
            "email", signupCommand.email(),
            "firstName", signupCommand.firstName(),
            "lastName", signupCommand.lastName(),
            "verificationCode", verificationCode));

    return authResultMapper.toVerifyAuthUserByCodeResult(tokenPair);
  }

  @Transactional
  public LoginResult login(LoginCommand loginCommand) {

    AuthUserEntity authUser =
        authUserRepository
            .findByEmail(loginCommand.email())
            .orElseThrow(() -> new InvalidEmailOrPasswordException());

    boolean matches = passwordEncoder.matches(loginCommand.password(), authUser.getPasswordHash());

    if (!matches) {
      throw new InvalidEmailOrPasswordException();
    }

    if (authUser.getStatus() != AuthUserStatus.ACTIVE) {
      throw new AuthUserNotActiveException(authUser.getId());
    }

    UserRoleEntity userRole =
        userRoleRepository
            .findByAuthUserId(authUser.getId())
            .orElseThrow(() -> new RoleNotFoundException(authUser.getId()));

    TokenPair tokenPair = issueTokenPair(authUser, userRole.getRole().getName());

    return authResultMapper.toLoginResult(tokenPair);
  }

  @Transactional
  public void logout(LogoutCommand logoutCommand) {
    String logoutTokenHash = tokenService.hashToken(logoutCommand.refreshToken());
    RefreshTokenEntity refreshTokenEntity =
        refreshTokenRepository
            .findByTokenHash(logoutTokenHash)
            .orElseThrow(() -> new RefreshTokenNotFoundException());

    if (refreshTokenEntity.getRevokedAt() != null) {
      throw new RefreshTokenAlreadyRevokedException();
    }

    if (refreshTokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new RefreshTokenAlreadyExpiredException();
    }

    refreshTokenEntity.setRevokedAt(LocalDateTime.now());
    refreshTokenRepository.save(refreshTokenEntity);
  }

  @Transactional
  public RefreshResult refresh(RefreshCommand refreshCommand) {
    String logoutTokenHash = tokenService.hashToken(refreshCommand.refreshToken());
    RefreshTokenEntity refreshTokenEntity =
        refreshTokenRepository
            .findByTokenHashForUpdate(logoutTokenHash)
            .orElseThrow(() -> new RefreshTokenNotFoundException());

    if (refreshTokenEntity.getRevokedAt() != null) {
      throw new RefreshTokenAlreadyRevokedException();
    }

    if (refreshTokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new RefreshTokenAlreadyExpiredException();
    }

    AuthUserEntity authUser = refreshTokenEntity.getAuthUser();

    refreshTokenEntity.setRevokedAt(LocalDateTime.now());
    refreshTokenRepository.save(refreshTokenEntity);

    UserRoleEntity userRole =
        userRoleRepository
            .findByAuthUserId(authUser.getId())
            .orElseThrow(() -> new RoleNotFoundException(authUser.getId()));

    TokenPair tokenPair = issueTokenPair(authUser, userRole.getRole().getName());

    return authResultMapper.toRefreshResult(tokenPair);
  }

  public void changePassword(ChangePasswordCommand changePasswordCommand) {

    AuthUserEntity authUser =
        authUserRepository
            .findById(changePasswordCommand.authUserId())
            .orElseThrow(() -> new AuthUserNotFoundException(changePasswordCommand.authUserId()));

    if (!passwordEncoder.matches(changePasswordCommand.oldPassword(), authUser.getPasswordHash())) {
      throw new InvalidOldPasswordException();
    }

    authUser.setPasswordHash(passwordEncoder.encode(changePasswordCommand.newPassword()));
    authUserRepository.save(authUser);
  }

  public void blockUser(BlockAuthUserCommand blockAuthUserCommand) {
    AuthUserEntity authUser =
        authUserRepository
            .findById(blockAuthUserCommand.authUserId())
            .orElseThrow(() -> new AuthUserNotFoundException(blockAuthUserCommand.authUserId()));

    if (authUser.getStatus() == AuthUserStatus.BLOCKED) {
      throw new AuthUserAlreadyBlockedException(authUser.getId());
    }

    authUser.setStatus(AuthUserStatus.BLOCKED);
    authUserRepository.save(authUser);

    saveAuthOutboxEvent(
        authUser.getId(),
        AuthEventType.AUTH_USER_BLOCKED,
        Map.of(
            "authUserId", authUser.getId(),
            "email", authUser.getEmail()));
  }

  public void unlockUser(UnlockAuthUserCommand unlockAuthUserCommand) {
    AuthUserEntity authUser =
        authUserRepository
            .findById(unlockAuthUserCommand.authUserId())
            .orElseThrow(() -> new AuthUserNotFoundException(unlockAuthUserCommand.authUserId()));

    if (authUser.getStatus() == AuthUserStatus.ACTIVE) {
      throw new AuthUserAlreadyActiveException(authUser.getId());
    }

    authUser.setStatus(AuthUserStatus.ACTIVE);
    authUserRepository.save(authUser);

    saveAuthOutboxEvent(
        authUser.getId(),
        AuthEventType.AUTH_USER_UNLOCK,
        Map.of(
            "authUserId", authUser.getId(),
            "email", authUser.getEmail()));
  }

  @Transactional
  public void changeAuthUserRole(ChangeAuthUserRoleCommand changeAuthUserRoleCommand) {
    UserRoleEntity userRole =
        userRoleRepository
            .findByAuthUserId(changeAuthUserRoleCommand.authUserId())
            .orElseThrow(() -> new RoleNotFoundException(changeAuthUserRoleCommand.authUserId()));

    RoleEntity role =
        roleRepository
            .findByName(changeAuthUserRoleCommand.role())
            .orElseThrow(() -> new RoleNotFoundException(changeAuthUserRoleCommand.authUserId()));

    if (userRole.getRole().getName() == role.getName()) {
      return;
    }

    userRole.setRole(role);
    userRoleRepository.save(userRole);

    saveAuthOutboxEvent(
        changeAuthUserRoleCommand.authUserId(),
        AuthEventType.AUTH_USER_ROLE_CHANGED,
        Map.of(
            "authUserId", changeAuthUserRoleCommand.authUserId(),
            "role", role.getName().name()));
  }

  @Transactional
  public VerifyAuthUserByCodeResult verifyByCode(
      VerifyAuthUserByCodeCommand verifyAuthUserCommand) {
    AuthUserEntity authUser = getNotVerifiedAuthUser(verifyAuthUserCommand.authUserId());

    if (verifyAuthUserCommand.verificationCode() == null
        || verifyAuthUserCommand.verificationCode().isBlank()
        || !passwordEncoder.matches(
            verifyAuthUserCommand.verificationCode(), authUser.getVerificationCodeHash())) {
      throw new InvalidVerificationCodeException();
    }

    activateVerifiedAuthUser(authUser);
    saveAuthUserVerifiedOutboxEvent(authUser);

    UserRoleEntity userRole =
        userRoleRepository
            .findByAuthUserId(authUser.getId())
            .orElseThrow(() -> new RoleNotFoundException(authUser.getId()));

    TokenPair tokenPair = issueTokenPair(authUser, userRole.getRole().getName());

    return authResultMapper.toVerifyAuthUserByCodeResult(tokenPair);
  }

  @Transactional
  public void verifyByPrivilegedRole(VerifyAuthUserByPrivilegeRoleCommand verifyAuthUserCommand) {
    if (!canVerifyWithoutCode(verifyAuthUserCommand.role().name())) {
      throw new VerificationByRoleNotAllowedException();
    }

    AuthUserEntity authUser = getNotVerifiedAuthUser(verifyAuthUserCommand.authUserId());
    activateVerifiedAuthUser(authUser);
    saveAuthUserVerifiedOutboxEvent(authUser);
  }

  private AuthUserEntity getNotVerifiedAuthUser(UUID authUserId) {
    AuthUserEntity authUser =
        authUserRepository
            .findById(authUserId)
            .orElseThrow(() -> new AuthUserNotFoundException(authUserId));

    if (authUser.isEmailVerified()) {
      throw new AuthUserAlreadyVerifiedException();
    }

    return authUser;
  }

  private void activateVerifiedAuthUser(AuthUserEntity authUser) {
    authUser.setEmailVerified(true);
    authUser.setStatus(AuthUserStatus.ACTIVE);
    authUserRepository.save(authUser);
  }

  private void saveAuthUserVerifiedOutboxEvent(AuthUserEntity authUser) {
    saveAuthOutboxEvent(
        authUser.getId(),
        AuthEventType.AUTH_USER_VERIFIED,
        Map.of(
            "authUserId", authUser.getId(),
            "email", authUser.getEmail()));
  }

  private boolean canVerifyWithoutCode(String role) {
    return Roles.ADMIN.name().equals(role) || Roles.MANAGER.name().equals(role);
  }

  public GetAuthUserByIdResult getAuthUserById(GetAuthUserByIdCommand command) {
    AuthUserEntity authUser =
        authUserRepository
            .findById(command.authUserId())
            .orElseThrow(() -> new AuthUserNotFoundException(command.authUserId()));
    UserRoleEntity userRole =
        userRoleRepository
            .findByAuthUserId(command.authUserId())
            .orElseThrow(() -> new RoleNotFoundException(command.authUserId()));

    return authResultMapper.toGetAuthUserByIdResult(authUser, userRole);
  }

  @Transactional
  public void forgetPassword(ForgetPasswordCommand command) {
    AuthUserEntity authUser =
        authUserRepository
            .findByEmail(command.email())
            .orElseThrow(() -> new AuthUserNotFoundByEmailException(command.email()));
    List<RefreshTokenEntity> refreshTokenEntityList =
        refreshTokenRepository.findAllByAuthUserId(authUser.getId());
    LocalDateTime now = LocalDateTime.now();
    refreshTokenEntityList.stream()
        .filter(token -> token.getRevokedAt() == null)
        .forEach(token -> token.setRevokedAt(now));

    refreshTokenRepository.saveAll(refreshTokenEntityList);

    authUser.setStatus(AuthUserStatus.FORGET_PASSWORD);
    authUserRepository.save(authUser);
    saveAuthUserForgetPasswordOutboxEvent(authUser);
  }

  public ResetPasswordResult resetPassword(ResetPasswordCommand command) {
    AuthUserEntity authUser =
        authUserRepository
            .findById(command.authUserId())
            .orElseThrow(() -> new AuthUserNotFoundException(command.authUserId()));

    if (authUser.getStatus() != AuthUserStatus.FORGET_PASSWORD) {
      throw new AuthUserMustBeInForgetPasswordStatusException(authUser.getId());
    }

    authUser.setPasswordHash(passwordEncoder.encode(command.newPassword()));
    authUser.setStatus(AuthUserStatus.ACTIVE);

    authUserRepository.save(authUser);

    UserRoleEntity userRole =
        userRoleRepository
            .findByAuthUserId(authUser.getId())
            .orElseThrow(() -> new RoleNotFoundException(authUser.getId()));

    TokenPair tokenPair = issueTokenPair(authUser, userRole.getRole().getName());

    return authResultMapper.toResetPasswordResult(tokenPair);
  }

  private TokenPair issueTokenPair(AuthUserEntity authUser, Roles role) {
    String accessToken = tokenService.generateAccessToken(authUser, role).getTokenValue();
    String refreshToken = tokenService.generateRefreshToken();
    String refreshTokenHash = tokenService.hashToken(refreshToken);

    RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
    refreshTokenEntity.setAuthUser(authUser);
    refreshTokenEntity.setTokenHash(refreshTokenHash);
    refreshTokenEntity.setExpiresAt(tokenService.refreshTokenExpiresAt());
    refreshTokenRepository.save(refreshTokenEntity);

    return new TokenPair(
        accessToken,
        refreshToken,
        jwtProperties.accessTokenTtlMinutes(),
        jwtProperties.refreshTokenTtlDays());
  }

  private void saveAuthUserForgetPasswordOutboxEvent(AuthUserEntity authUser) {
    saveAuthOutboxEvent(
        authUser.getId(),
        AuthEventType.AUTH_USER_FORGET_PASSWORD,
        Map.of(
            "authUserId", authUser.getId(),
            "email", authUser.getEmail()));
  }

  private AuthOutboxEventEntity saveAuthOutboxEvent(
      UUID authUserId, AuthEventType eventType, Map<String, Object> payload) {
    var authOutboxEvent = new AuthOutboxEventEntity();
    authOutboxEvent.setAggregateType("AUTH_USER");
    authOutboxEvent.setAggregateId(authUserId);
    authOutboxEvent.setEventType(eventType.name());
    authOutboxEvent.setTopic(eventType.getTopic());
    authOutboxEvent.setEventKey(authUserId + ":" + eventType.name());
    authOutboxEvent.setSchemaVersion(eventType.getVersion());
    authOutboxEvent.setPayload(payload);
    return authOutboxEventRepository.save(authOutboxEvent);
  }

  @Transactional
  public SocialLoginResult socialLogin(SocialLoginCommand command) {
    var socialAccount =
        authSocialAccountsRepository.findByProviderAndProviderUserId(
            command.provider(), command.providerUserId());

    if (socialAccount.isPresent()) {
      var authUserFromSocialAccount = socialAccount.get().getAuthUser();
      UserRoleEntity userRole =
          userRoleRepository
              .findByAuthUserId(authUserFromSocialAccount.getId())
              .orElseThrow(() -> new RoleNotFoundException(authUserFromSocialAccount.getId()));
      var tokens = issueTokenPair(authUserFromSocialAccount, userRole.getRole().getName());
      return authResultMapper.toSocialLoginResult(tokens);
    }

    var authUser = authUserRepository.findByEmail(command.email());
    if (authUser.isPresent()) {
      saveAuthSocialAccount(command, authUser.get());
      UserRoleEntity userRole =
          userRoleRepository
              .findByAuthUserId(authUser.get().getId())
              .orElseThrow(() -> new RoleNotFoundException(authUser.get().getId()));
      var tokens = issueTokenPair(authUser.get(), userRole.getRole().getName());
      return authResultMapper.toSocialLoginResult(tokens);
    }

    var newAuthUser = new AuthUserEntity();
    newAuthUser.setStatus(AuthUserStatus.ACTIVE);
    newAuthUser.setEmail(command.email());
    newAuthUser.setEmailVerified(command.isEmailVerified());
    newAuthUser = authUserRepository.save(newAuthUser);

    var newUserRole = new UserRoleEntity();

    AuthUserEntity finalNewAuthUser = newAuthUser;

    RoleEntity roleEntity =
        roleRepository
            .findByName(Roles.USER)
            .orElseThrow(() -> new RoleNotFoundException(finalNewAuthUser.getId()));

    newUserRole.setAuthUser(newAuthUser);
    newUserRole.setRole(roleEntity);
    userRoleRepository.save(newUserRole);

    saveAuthSocialAccount(command, newAuthUser);

    var tokens = issueTokenPair(newAuthUser, newUserRole.getRole().getName());

    saveAuthOutboxEvent(
        newAuthUser.getId(),
        AuthEventType.AUTH_SOCIAL_ACCOUNT_AUTH_USER_CREATED,
        Map.of(
            "authUserId", newAuthUser.getId(),
            "email", newAuthUser.getEmail(),
            "firstName", command.firstName(),
            "lastName", command.lastName()));

    return authResultMapper.toSocialLoginResult(tokens);
  }

  private AuthSocialAccountsEntity saveAuthSocialAccount(
      SocialLoginCommand command, AuthUserEntity authUser) {
    var authSocialAccount = new AuthSocialAccountsEntity();
    authSocialAccount.setProvider(command.provider());
    authSocialAccount.setProviderUserId(command.providerUserId());
    authSocialAccount.setAuthUser(authUser);
    return authSocialAccountsRepository.save(authSocialAccount);
  }
}
