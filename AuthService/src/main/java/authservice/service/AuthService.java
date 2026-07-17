package authservice.service;

import authservice.config.JwtProperties;
import authservice.dto.*;
import authservice.entity.*;
import authservice.enums.AuthOutboxEvents;
import authservice.enums.Roles;
import authservice.exception.EmailAlreadyExistsException;
import authservice.exception.InvalidEmailOrPasswordException;
import authservice.exception.RefreshTokenNotFoundException;
import authservice.exception.RoleNotFoundException;
import authservice.repository.*;
import jakarta.persistence.LockModeType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {
    private final AuthUserRepository authUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final AuthOutboxEventRepository authOutboxEventRepository;

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

    @Transactional
    public SignupResult signup(SignupCommand signupCommand) {
        if (authUserRepository.existsByEmail(signupCommand.email())) {
            throw new EmailAlreadyExistsException(signupCommand.email());
        }


        AuthUserEntity userEntity = new AuthUserEntity();
        userEntity.setEmail(signupCommand.email());
        userEntity.setPasswordHash(passwordEncoder.encode(signupCommand.password()));

        AuthUserEntity savedUser;
        try {
            savedUser = authUserRepository.saveAndFlush(userEntity);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException(signupCommand.email());
        }

        RoleEntity roleEntity = roleRepository.findByName(Roles.USER)
                .orElseThrow(() -> new IllegalStateException("Role USER not found"));

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
        authOutboxEvent.setEventType(AuthOutboxEvents.AUTH_USER_CREATED);
        authOutboxEvent.setTopic("auth.user.created");
        authOutboxEvent.setEventKey(savedUser.getId().toString());
        authOutboxEvent.setSchemaVersion("1");
        authOutboxEvent.setPayload(Map.of(
                "authUserId", savedUser.getId().toString(),
                "email", savedUser.getEmail().toString(),
                "firstName", signupCommand.firstName(),
                "lastName", signupCommand.lastName()
        ));
        authOutboxEventRepository.save(authOutboxEvent);

        return new SignupResult(accessToken, refreshToken, jwtProperties.accessTokenTtlMinutes(), jwtProperties.refreshTokenTtlDays());
    }

    @Transactional
    public LoginResult login(LoginCommand loginCommand) {

        AuthUserEntity authUser = authUserRepository.findByEmail(loginCommand.email())
                .orElseThrow(() -> new InvalidEmailOrPasswordException());

        boolean matches = passwordEncoder.matches(loginCommand.password(), authUser.getPasswordHash());

        if (!matches) {
            throw new InvalidEmailOrPasswordException();
        }

        UserRoleEntity userRole = userRoleRepository.findByAuthUserId(authUser.getId()).orElseThrow(() ->
                new RoleNotFoundException());

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
            throw new IllegalArgumentException("Refresh token already revoked");
        }

        if (refreshTokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token already expired");
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
            throw new IllegalArgumentException("Refresh token already revoked");
        }

        if (refreshTokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token already expired");
        }

        AuthUserEntity authUser = refreshTokenEntity.getAuthUser();

        refreshTokenEntity.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshTokenEntity);

        UserRoleEntity userRole = userRoleRepository
                .findByAuthUserId(authUser.getId())
                .orElseThrow(() ->
                        new RoleNotFoundException());

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

}
