package authservice.mapper;

import auth.contract.v1.*;
import authservice.dto.*;
import org.mapstruct.Mapper;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AuthMapper {
    default SignupCommand toSignupCommand(SignupAuthGrpcRequest signupAuthGrpcRequest) {
        return new SignupCommand(
                signupAuthGrpcRequest.getEmail(),
                signupAuthGrpcRequest.getPassword(),
                signupAuthGrpcRequest.getFirstName(),
                signupAuthGrpcRequest.getLastName()
        );
    }

    default SignupAuthGrpcResponse toSignupGrpcResponse(SignupResult signupResult) {
        return SignupAuthGrpcResponse.newBuilder()
                .setRefreshToken(signupResult.refreshToken())
                .setAccessToken(signupResult.accessToken())
                .setAccessTokenMinutesTtl((int) signupResult.accessTokenMinutesTtl())
                .setRefreshTokenDaysTtl((int) signupResult.refreshTokenDaysTtl())
                .build();
    }

    default LoginCommand toLoginCommand(LoginAuthGrpcRequest loginAuthGrpcRequest) {
        return new LoginCommand(
                loginAuthGrpcRequest.getEmail(),
                loginAuthGrpcRequest.getPassword()
        );
    }

    default LoginAuthGrpcResponse toLoginGrpcResponse(LoginResult loginResult) {
        return LoginAuthGrpcResponse.newBuilder()
                .setRefreshToken(loginResult.refreshToken())
                .setAccessToken(loginResult.accessToken())
                .setAccessTokenMinutesTtl((int) loginResult.accessTokenMinutesTtl())
                .setRefreshTokenDaysTtl((int) loginResult.refreshTokenDaysTtl())
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
                .setRefreshToken(refreshResult.refreshToken())
                .setAccessToken(refreshResult.accessToken())
                .setAccessTokenMinutesTtl((int) refreshResult.accessTokenMinutesTtl())
                .setRefreshTokenDaysTtl((int) refreshResult.refreshTokenDaysTtl())
                .build();
    }
}
