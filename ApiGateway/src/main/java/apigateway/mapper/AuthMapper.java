package apigateway.mapper;

import apigateway.dto.auth.*;
import auth.contract.v1.*;
import org.mapstruct.Mapper;

import java.util.Locale;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    default SignupAuthGrpcRequest toSignupAuthGrpcRequest(SignupRequestDto request) {
        return SignupAuthGrpcRequest.newBuilder()
                .setEmail(request.email().trim().toLowerCase(Locale.ROOT))
                .setPassword(request.password())
                .setFirstName(request.firstName())
                .setLastName(request.lastName())
                .build();
    }

    default SignupResponseDto toSignupResponseDto(SignupAuthGrpcResponse authResponse) {
        return new SignupResponseDto(
                authResponse.getAccessToken(),
                authResponse.getRefreshToken(),
                authResponse.getAccessTokenMinutesTtl(),
                authResponse.getRefreshTokenDaysTtl()
        );
    }

    default LoginAuthGrpcRequest toLoginAuthGrpcRequest(LoginRequestDto request) {
        return LoginAuthGrpcRequest.newBuilder()
                .setEmail(request.email().trim().toLowerCase(Locale.ROOT))
                .setPassword(request.password())
                .build();
    }

    default LoginResponseDto toLoginResponseDto(LoginAuthGrpcResponse authResponse) {
        return new LoginResponseDto(
                authResponse.getAccessToken(),
                authResponse.getRefreshToken(),
                authResponse.getAccessTokenMinutesTtl(),
                authResponse.getRefreshTokenDaysTtl()
        );
    }

    default LogoutAuthGrpcRequest toLogoutAuthGrpcRequest(LogoutRequestDto request) {
        return LogoutAuthGrpcRequest.newBuilder()
                .setRefreshToken(request.refreshToken())
                .build();
    }

    default RefreshAuthGrpcRequest toRefreshAuthGrpcRequest(RefreshRequestDto request) {
        return RefreshAuthGrpcRequest.newBuilder()
                .setRefreshToken(request.refreshToken())
                .build();
    }

    default RefreshResponseDto toRefreshResponseDto (RefreshAuthGrpcResponse response) {
        return new RefreshResponseDto(
                response.getAccessToken(),
                response.getRefreshToken(),
                response.getAccessTokenMinutesTtl(),
                response.getRefreshTokenDaysTtl()
        );
    }
}
