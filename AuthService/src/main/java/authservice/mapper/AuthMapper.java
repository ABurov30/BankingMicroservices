package authservice.mapper;

import auth.contract.v1.*;
import authservice.dto.*;
import kafkacontracts.auth.AuthUserBlockedEventPayload;
import kafkacontracts.auth.AuthUserCreatedEventPayload;
import kafkacontracts.auth.AuthUserUnlockEventPayload;
import kafkacontracts.auth.AuthUserVerifiedEventPayload;
import org.mapstruct.Mapper;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AuthMapper {
    default SignupCommand toSignupCommand(SignupAuthGrpcRequest signupAuthGrpcRequest) {
        return new SignupCommand(
                signupAuthGrpcRequest.getEmail().trim().toLowerCase(Locale.ROOT),
                signupAuthGrpcRequest.getPassword(),
                signupAuthGrpcRequest.getFirstName(),
                signupAuthGrpcRequest.getLastName()
        );
    }

    default SignupAuthGrpcResponse toSignupGrpcResponse(SignupResult signupResult) {
        return SignupAuthGrpcResponse.newBuilder()
                .setTokens(toAuthTokenResponse(signupResult))
                .build();
    }

    default LoginCommand toLoginCommand(LoginAuthGrpcRequest loginAuthGrpcRequest) {
        return new LoginCommand(
                loginAuthGrpcRequest.getEmail().trim().toLowerCase(Locale.ROOT),
                loginAuthGrpcRequest.getPassword()
        );
    }

    default LoginAuthGrpcResponse toLoginGrpcResponse(LoginResult loginResult) {
        return LoginAuthGrpcResponse.newBuilder()
                .setTokens(toAuthTokenResponse(loginResult))
                .build();
    }

    default LogoutCommand toLogoutCommand(LogoutAuthGrpcRequest logoutAuthGrpcRequest) {
        return new LogoutCommand(
                logoutAuthGrpcRequest.getRefreshToken()
        );
    }

    default RefreshCommand toRefreshCommand(RefreshAuthGrpcRequest refreshAuthGrpcRequest) {
        return new RefreshCommand(
                refreshAuthGrpcRequest.getRefreshToken()
        );
    }

    default RefreshAuthGrpcResponse toRefreshGrpcResponse(RefreshResult refreshResult) {
        return RefreshAuthGrpcResponse.newBuilder()
                .setTokens(toAuthTokenResponse(refreshResult))
                .build();
    }

    private AuthTokenResponse toAuthTokenResponse(SignupResult signupResult) {
        return AuthTokenResponse.newBuilder()
                .setRefreshToken(signupResult.refreshToken())
                .setAccessToken(signupResult.accessToken())
                .setAccessTokenMinutesTtl((int) signupResult.accessTokenMinutesTtl())
                .setRefreshTokenDaysTtl((int) signupResult.refreshTokenDaysTtl())
                .build();
    }

    private AuthTokenResponse toAuthTokenResponse(LoginResult loginResult) {
        return AuthTokenResponse.newBuilder()
                .setRefreshToken(loginResult.refreshToken())
                .setAccessToken(loginResult.accessToken())
                .setAccessTokenMinutesTtl((int) loginResult.accessTokenMinutesTtl())
                .setRefreshTokenDaysTtl((int) loginResult.refreshTokenDaysTtl())
                .build();
    }

    private AuthTokenResponse toAuthTokenResponse(RefreshResult refreshResult) {
        return AuthTokenResponse.newBuilder()
                .setRefreshToken(refreshResult.refreshToken())
                .setAccessToken(refreshResult.accessToken())
                .setAccessTokenMinutesTtl((int) refreshResult.accessTokenMinutesTtl())
                .setRefreshTokenDaysTtl((int) refreshResult.refreshTokenDaysTtl())
                .build();
    }

    default AuthUserCreatedEventPayload toAuthUserCreatedEventPayload(Map<String, Object> payload) {
        return AuthUserCreatedEventPayload.newBuilder()
                .setAuthUserId(UUID.fromString(payload.get("authUserId").toString()))
                .setEmail(payload.get("email").toString())
                .setFirstName(payload.get("firstName").toString())
                .setLastName(payload.get("lastName").toString())
                .setVerificationCode(payload.get("verificationCode").toString())
                .build();
    }

    default AuthUserBlockedEventPayload toAuthUserBlockedEventPayload(Map<String, Object> payload) {
        return AuthUserBlockedEventPayload.newBuilder()
                .setAuthUserId(UUID.fromString(payload.get("authUserId").toString()))
                .setEmail(payload.get("email").toString())
                .build();
    }

    default AuthUserUnlockEventPayload toAuthUserUnlockEventPayload(Map<String, Object> payload) {
        return AuthUserUnlockEventPayload.newBuilder()
                .setAuthUserId(UUID.fromString(payload.get("authUserId").toString()))
                .setEmail(payload.get("email").toString())
                .build();
    }

    default AuthUserVerifiedEventPayload toAuthUserVerifiedEventPayload(Map<String, Object> payload) {
        return AuthUserVerifiedEventPayload.newBuilder()
                .setAuthUserId(UUID.fromString(payload.get("authUserId").toString()))
                .setEmail(payload.get("email").toString())
                .build();
    }

    default ChangePasswordCommand toChangePasswordCommand(ChangePasswordGrpcRequest request) {
        return new ChangePasswordCommand(
                UUID.fromString(request.getAuthUserId()),
                request.getOldPassword(),
                request.getNewPassword()
        );
    }

    default BlockAuthUserCommand toBlockAuthUserCommand(BlockAuthGrpcRequest request) {
        return new BlockAuthUserCommand(UUID.fromString(request.getAuthUserId()));
    }

    default UnlockAuthUserCommand toUnlockAuthUserCommand(UnlockAuthGrpcRequest request) {
        return new UnlockAuthUserCommand(UUID.fromString(request.getAuthUserId()));
    }

    default VerifyAuthUserCommand toVerifyAuthUserCommand(VerifyAuthGrpcRequest request) {
        return new VerifyAuthUserCommand(
                UUID.fromString(request.getAuthUserId()),
                request.hasVerificationCode() ? request.getVerificationCode() : null,
                request.hasRole() ? request.getRole() : null
        );
    }
}
