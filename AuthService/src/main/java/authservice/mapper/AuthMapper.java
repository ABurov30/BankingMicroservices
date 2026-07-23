package authservice.mapper;

import auth.contract.v1.*;
import authservice.dto.*;
import org.mapstruct.Mapper;

import java.util.Locale;
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
}
