package authservice.service;

import authservice.config.JwtProperties;
import authservice.dto.*;
import authservice.entity.*;
import authservice.exception.*;
import enums.auth.AuthUserStatus;
import enums.auth.Roles;
import authservice.repository.*;
import kafkacontracts.auth.AuthEventType;
import kafkacontracts.common.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    public AuthService(
            AuthUserRepository authUserRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtProperties jwtProperties,
            AuthOutboxEventRepository authOutboxEventRepository
    ) {
        this.authUserRepository = authUserRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
        this.authOutboxEventRepository = authOutboxEventRepository;
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

        RoleEntity roleEntity = roleRepository.findByName(Roles.USER)
                .orElseThrow(() -> new RoleNotFoundException(savedUser.getId()));

        UserRoleEntity userRoleEntity = new UserRoleEntity();
        userRoleEntity.setAuthUser(savedUser);
        userRoleEntity.setRole(roleEntity);
        UserRoleEntity savedUserRole = userRoleRepository.save(userRoleEntity);

        String accessToken = tokenService.generateAccessToken(savedUser, savedUserRole.getRole().getName())
                .getTokenValue();
        String refreshToken = tokenService.generateRefreshToken();
        String refreshTokenHash = tokenService.hashToken(refreshToken);

        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setAuthUser(savedUser);
        refreshTokenEntity.setTokenHash(refreshTokenHash);
        refreshTokenEntity.setExpiresAt(tokenService.refreshTokenExpiresAt());
        refreshTokenRepository.save(refreshTokenEntity);

        AuthOutboxEventEntity authOutboxEvent = new AuthOutboxEventEntity();
        authOutboxEvent.setAggregateType("AUTH_USER");
        authOutboxEvent.setAggregateId(savedUser.getId());
        authOutboxEvent.setEventType(AuthEventType.AUTH_USER_CREATED.name());
        authOutboxEvent.setTopic(AuthEventType.AUTH_USER_CREATED.getTopic());
        authOutboxEvent.setEventKey(savedUser.getId() + ":" + AuthEventType.AUTH_USER_CREATED.name());
        authOutboxEvent.setSchemaVersion(AuthEventType.AUTH_USER_CREATED.getVersion());

        authOutboxEvent.setPayload(Map.of(
                "authUserId", savedUser.getId(),
                "email", signupCommand.email(),
                "firstName", signupCommand.firstName(),
                "lastName", signupCommand.lastName(),
                "verificationCode", verificationCode
        ));

        authOutboxEventRepository.save(authOutboxEvent);

        return new VerifyAuthUserByCodeResult(accessToken, refreshToken, jwtProperties.accessTokenTtlMinutes(), jwtProperties.refreshTokenTtlDays());
    }

    @Transactional
    public LoginResult login(LoginCommand loginCommand) {

        AuthUserEntity authUser = authUserRepository.findByEmail(loginCommand.email())
                .orElseThrow(() -> new InvalidEmailOrPasswordException());

        boolean matches = passwordEncoder.matches(loginCommand.password(), authUser.getPasswordHash());

        if (!matches) {
            throw new InvalidEmailOrPasswordException();
        }

        if (authUser.getStatus() != AuthUserStatus.ACTIVE) {
            throw new AuthUserNotActiveException(authUser.getId());
        }

        UserRoleEntity userRole = userRoleRepository.findByAuthUserId(authUser.getId()).orElseThrow(() ->
                new RoleNotFoundException(authUser.getId()));

        String accessToken = tokenService.generateAccessToken(authUser, userRole.getRole().getName()).getTokenValue();
        String refreshToken = tokenService.generateRefreshToken();
        String refreshTokenHash = tokenService.hashToken(refreshToken);

        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setAuthUser(authUser);
        refreshTokenEntity.setTokenHash(refreshTokenHash);
        refreshTokenEntity.setExpiresAt(tokenService.refreshTokenExpiresAt());
        refreshTokenRepository.save(refreshTokenEntity);

        return new LoginResult(
                accessToken,
                refreshToken,
                jwtProperties.accessTokenTtlMinutes(),
                jwtProperties.refreshTokenTtlDays()
        );
    }

    @Transactional
    public void logout(LogoutCommand logoutCommand) {
        String logoutTokenHash = tokenService.hashToken(logoutCommand.refreshToken());
        RefreshTokenEntity refreshTokenEntity = refreshTokenRepository.findByTokenHash(logoutTokenHash)
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
        RefreshTokenEntity refreshTokenEntity = refreshTokenRepository.findByTokenHashForUpdate(logoutTokenHash)
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

        UserRoleEntity userRole = userRoleRepository
                .findByAuthUserId(authUser.getId())
                .orElseThrow(() ->
                        new RoleNotFoundException(authUser.getId()));

        String accessToken = tokenService.generateAccessToken(authUser, userRole.getRole().getName())
                .getTokenValue();

        RefreshTokenEntity newRefreshToken = new RefreshTokenEntity();
        String refreshToken = tokenService.generateRefreshToken();
        String refreshTokenHash = tokenService.hashToken(refreshToken);
        newRefreshToken.setExpiresAt(tokenService.refreshTokenExpiresAt());
        newRefreshToken.setTokenHash(refreshTokenHash);
        newRefreshToken.setAuthUser(authUser);
        refreshTokenRepository.save(newRefreshToken);

        return new RefreshResult(
                accessToken,
                refreshToken,
                jwtProperties.accessTokenTtlMinutes(),
                jwtProperties.refreshTokenTtlDays()
        );
    }

    public void changePassword(ChangePasswordCommand changePasswordCommand) {

        AuthUserEntity authUser = authUserRepository.findById(changePasswordCommand.authUserId())
                .orElseThrow(() -> new AuthUserNotFoundException(changePasswordCommand.authUserId()));

        if (!passwordEncoder.matches(changePasswordCommand.oldPassword(), authUser.getPasswordHash())) {
            throw new InvalidOldPasswordException();
        }

        authUser.setPasswordHash(passwordEncoder.encode(changePasswordCommand.newPassword()));
        authUserRepository.save(authUser);
    }

    public void blockUser(BlockAuthUserCommand blockAuthUserCommand) {
        AuthUserEntity authUser = authUserRepository.findById(blockAuthUserCommand.authUserId())
                .orElseThrow(() -> new AuthUserNotFoundException(blockAuthUserCommand.authUserId()));

        if (authUser.getStatus() == AuthUserStatus.BLOCKED) {
            throw new AuthUserAlreadyBlockedException(authUser.getId());
        }

        authUser.setStatus(AuthUserStatus.BLOCKED);
        authUserRepository.save(authUser);

        AuthOutboxEventEntity authOutboxEvent = new AuthOutboxEventEntity();
        authOutboxEvent.setAggregateType("AUTH_USER");
        authOutboxEvent.setAggregateId(authUser.getId());
        authOutboxEvent.setEventType(AuthEventType.AUTH_USER_BLOCKED.name());
        authOutboxEvent.setTopic(AuthEventType.AUTH_USER_BLOCKED.getTopic());
        authOutboxEvent.setEventKey(authUser.getId() + ":" + AuthEventType.AUTH_USER_BLOCKED.name());
        authOutboxEvent.setSchemaVersion(AuthEventType.AUTH_USER_BLOCKED.getVersion());

        authOutboxEvent.setPayload(Map.of(
                "authUserId", authUser.getId(),
                "email", authUser.getEmail()
        ));

        authOutboxEventRepository.save(authOutboxEvent);
    }

    public void unlockUser(UnlockAuthUserCommand unlockAuthUserCommand) {
        AuthUserEntity authUser = authUserRepository.findById(unlockAuthUserCommand.authUserId())
                .orElseThrow(() -> new AuthUserNotFoundException(unlockAuthUserCommand.authUserId()));

        if (authUser.getStatus() == AuthUserStatus.ACTIVE) {
            throw new AuthUserAlreadyActiveException(authUser.getId());
        }

        authUser.setStatus(AuthUserStatus.ACTIVE);
        authUserRepository.save(authUser);

        AuthOutboxEventEntity authOutboxEvent = new AuthOutboxEventEntity();
        authOutboxEvent.setAggregateType("AUTH_USER");
        authOutboxEvent.setAggregateId(authUser.getId());
        authOutboxEvent.setEventType(AuthEventType.AUTH_USER_UNLOCK.name());
        authOutboxEvent.setTopic(AuthEventType.AUTH_USER_UNLOCK.getTopic());
        authOutboxEvent.setEventKey(authUser.getId() + ":" + AuthEventType.AUTH_USER_UNLOCK.name());
        authOutboxEvent.setSchemaVersion(AuthEventType.AUTH_USER_UNLOCK.getVersion());

        authOutboxEvent.setPayload(Map.of(
                "authUserId", authUser.getId(),
                "email", authUser.getEmail()
        ));

        authOutboxEventRepository.save(authOutboxEvent);
    }

    @Transactional
    public void changeAuthUserRole(ChangeAuthUserRoleCommand changeAuthUserRoleCommand) {
        UserRoleEntity userRole = userRoleRepository
                .findByAuthUserId(changeAuthUserRoleCommand.authUserId())
                .orElseThrow(() -> new RoleNotFoundException(changeAuthUserRoleCommand.authUserId()));

        RoleEntity role = roleRepository
                .findByName(changeAuthUserRoleCommand.role())
                .orElseThrow(() -> new RoleNotFoundException(changeAuthUserRoleCommand.authUserId()));

        if (userRole.getRole().getName() == role.getName()) {
            return;
        }

        userRole.setRole(role);
        userRoleRepository.save(userRole);

        AuthOutboxEventEntity authOutboxEvent = new AuthOutboxEventEntity();
        authOutboxEvent.setAggregateType("AUTH_USER");
        authOutboxEvent.setAggregateId(changeAuthUserRoleCommand.authUserId());
        authOutboxEvent.setEventType(AuthEventType.AUTH_USER_ROLE_CHANGED.name());
        authOutboxEvent.setTopic(KafkaTopics.AUTH_USER_ROLE_CHANGED);
        authOutboxEvent.setEventKey(changeAuthUserRoleCommand.authUserId() + ":" + AuthEventType.AUTH_USER_ROLE_CHANGED.name());
        authOutboxEvent.setSchemaVersion(AuthEventType.AUTH_USER_ROLE_CHANGED.getVersion());

        authOutboxEvent.setPayload(Map.of(
                "authUserId", changeAuthUserRoleCommand.authUserId(),
                "role", role.getName().name()
        ));

        authOutboxEventRepository.save(authOutboxEvent);
    }

    @Transactional
    public VerifyAuthUserByCodeResult verifyByCode(VerifyAuthUserByCodeCommand verifyAuthUserCommand) {
        AuthUserEntity authUser = getNotVerifiedAuthUser(verifyAuthUserCommand.authUserId());

        if (
                verifyAuthUserCommand.verificationCode() == null ||
                        verifyAuthUserCommand.verificationCode().isBlank() ||
                        !passwordEncoder.matches(verifyAuthUserCommand.verificationCode(), authUser.getVerificationCodeHash())
        ) {
            throw new InvalidVerificationCodeException();
        }

        activateVerifiedAuthUser(authUser);
        saveAuthUserVerifiedOutboxEvent(authUser);

        UserRoleEntity userRole = userRoleRepository.findByAuthUserId(authUser.getId())
                .orElseThrow(() -> new RoleNotFoundException(authUser.getId()));

        String accessToken = tokenService.generateAccessToken(authUser, userRole.getRole().getName()).getTokenValue();
        String refreshToken = tokenService.generateRefreshToken();

        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setAuthUser(authUser);
        refreshTokenEntity.setTokenHash(tokenService.hashToken(refreshToken));
        refreshTokenEntity.setExpiresAt(tokenService.refreshTokenExpiresAt());
        refreshTokenRepository.save(refreshTokenEntity);

        return new VerifyAuthUserByCodeResult(
                accessToken,
                refreshToken,
                jwtProperties.accessTokenTtlMinutes(),
                jwtProperties.refreshTokenTtlDays()
        );
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
        AuthUserEntity authUser = authUserRepository.findById(authUserId)
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
        AuthOutboxEventEntity authOutboxEvent = new AuthOutboxEventEntity();
        authOutboxEvent.setAggregateType("AUTH_USER");
        authOutboxEvent.setAggregateId(authUser.getId());
        authOutboxEvent.setEventType(AuthEventType.AUTH_USER_VERIFIED.name());
        authOutboxEvent.setTopic(AuthEventType.AUTH_USER_VERIFIED.getTopic());
        authOutboxEvent.setEventKey(authUser.getId() + ":" + AuthEventType.AUTH_USER_VERIFIED.name());
        authOutboxEvent.setSchemaVersion(AuthEventType.AUTH_USER_VERIFIED.getVersion());

        authOutboxEvent.setPayload(Map.of(
                "authUserId", authUser.getId(),
                "email", authUser.getEmail()
        ));

        authOutboxEventRepository.save(authOutboxEvent);
    }

    private boolean canVerifyWithoutCode(String role) {
        return Roles.ADMIN.name().equals(role) || Roles.MANAGER.name().equals(role);
    }

    public GetAuthUserByIdResult getAuthUserById(GetAuthUserByIdCommand command) {
        AuthUserEntity authUser = authUserRepository.findById(command.authUserId())
                .orElseThrow(() -> new AuthUserNotFoundException(command.authUserId()));
        UserRoleEntity userRole = userRoleRepository.findByAuthUserId(command.authUserId())
                .orElseThrow(() -> new RoleNotFoundException(command.authUserId()));

        return new GetAuthUserByIdResult(
                authUser.getId(),
                authUser.getStatus(),
                authUser.getEmail(),
                userRole.getRole().getName()
        );
    }

    @Transactional
    public void forgetPassword(ForgetPasswordCommand command) {
        AuthUserEntity authUser = authUserRepository.findByEmail(command.email())
                .orElseThrow(() -> new AuthUserNotFoundByEmailException(command.email()));
        List<RefreshTokenEntity> refreshTokenEntityList = refreshTokenRepository.findAllByAuthUserId(authUser.getId());
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
        AuthUserEntity authUser = authUserRepository.findById(command.authUserId())
                .orElseThrow(() -> new AuthUserNotFoundException(command.authUserId()));

        if (authUser.getStatus() != AuthUserStatus.FORGET_PASSWORD) {
            throw new AuthUserMustBeInForgetPasswordStatusException(authUser.getId());
        }

        authUser.setPasswordHash(passwordEncoder.encode(command.newPassword()));
        authUser.setStatus(AuthUserStatus.ACTIVE);

        authUserRepository.save(authUser);

        UserRoleEntity userRole = userRoleRepository.findByAuthUserId(authUser.getId()).orElseThrow(() ->
                new RoleNotFoundException(authUser.getId()));

        String accessToken = tokenService.generateAccessToken(authUser, userRole.getRole().getName()).getTokenValue();
        String refreshToken = tokenService.generateRefreshToken();
        String refreshTokenHash = tokenService.hashToken(refreshToken);

        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setAuthUser(authUser);
        refreshTokenEntity.setTokenHash(refreshTokenHash);
        refreshTokenEntity.setExpiresAt(tokenService.refreshTokenExpiresAt());
        refreshTokenRepository.save(refreshTokenEntity);

        return new ResetPasswordResult(
                accessToken,
                refreshToken,
                jwtProperties.accessTokenTtlMinutes(),
                jwtProperties.refreshTokenTtlDays()
        );
    }

    private void saveAuthUserForgetPasswordOutboxEvent(AuthUserEntity authUser) {
        AuthOutboxEventEntity authOutboxEvent = new AuthOutboxEventEntity();
        authOutboxEvent.setAggregateType("AUTH_USER");
        authOutboxEvent.setAggregateId(authUser.getId());
        authOutboxEvent.setEventType(AuthEventType.AUTH_USER_FORGET_PASSWORD.name());
        authOutboxEvent.setTopic(AuthEventType.AUTH_USER_FORGET_PASSWORD.getTopic());
        authOutboxEvent.setEventKey(authUser.getId() + ":" + AuthEventType.AUTH_USER_FORGET_PASSWORD.name());
        authOutboxEvent.setSchemaVersion(AuthEventType.AUTH_USER_FORGET_PASSWORD.getVersion());

        authOutboxEvent.setPayload(Map.of(
                "authUserId", authUser.getId(),
                "email", authUser.getEmail()
        ));

        authOutboxEventRepository.save(authOutboxEvent);
    }

}
