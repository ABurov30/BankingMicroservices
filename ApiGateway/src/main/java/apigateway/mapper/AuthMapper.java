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

    default VerifyAuthUserByCodeResponseDto toVerifyAuthUserByCodeResponseDto(VerifyAuthUserByCodeGrpcResponse authResponse) {
        AuthTokenResponse tokens = authResponse.getTokens();

        return new VerifyAuthUserByCodeResponseDto(
                tokens.getAccessToken(),
                tokens.getRefreshToken(),
                tokens.getAccessTokenMinutesTtl(),
                tokens.getRefreshTokenDaysTtl()
        );
    }

    default LoginAuthGrpcRequest toLoginAuthGrpcRequest(LoginRequestDto request) {
        return LoginAuthGrpcRequest.newBuilder()
                .setEmail(request.email().trim().toLowerCase(Locale.ROOT))
                .setPassword(request.password())
                .build();
    }

    default LoginResponseDto toLoginResponseDto(LoginAuthGrpcResponse authResponse) {
        AuthTokenResponse tokens = authResponse.getTokens();

        return new LoginResponseDto(
                tokens.getAccessToken(),
                tokens.getRefreshToken(),
                tokens.getAccessTokenMinutesTtl(),
                tokens.getRefreshTokenDaysTtl()
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
        AuthTokenResponse tokens = response.getTokens();

        return new RefreshResponseDto(
                tokens.getAccessToken(),
                tokens.getRefreshToken(),
                tokens.getAccessTokenMinutesTtl(),
                tokens.getRefreshTokenDaysTtl()
        );
    }

    default ChangePasswordGrpcRequest toChangePasswordGrpcRequest (ChangePasswordRequestDto requestDto) {
        return ChangePasswordGrpcRequest.newBuilder()
                .setAuthUserId(requestDto.authUserId())
                .setNewPassword(requestDto.newPassword())
                .setOldPassword(requestDto.oldPassword())
                .build();
    }

    default BlockAuthGrpcRequest toBlockUserGrpcRequest (BlockAuthUserRequestDto requestDto) {
        return BlockAuthGrpcRequest.newBuilder()
                .setAuthUserId(requestDto.authUserId().toString())
                .build();
    }

    default UnlockAuthGrpcRequest toUnlockUserGrpcRequest (UnlockAuthUserRequestDto requestDto) {
        return UnlockAuthGrpcRequest.newBuilder()
                .setAuthUserId(requestDto.authUserId().toString())
                .build();
    }

    default ChangeAuthUserRoleGrpcRequest toChangeAuthUserRoleGrpcRequest(ChangeAuthUserRoleRequestDto requestDto) {
        return ChangeAuthUserRoleGrpcRequest.newBuilder()
                .setAuthUserId(requestDto.authUserId().toString())
                .setRole(requestDto.role().name())
                .build();
    }

    default VerifyAuthUserByCodeGrpcRequest toVerifyAuthUserByCodeGrpcRequest(VerifyAuthUserByCodeRequestDto request) {
        return VerifyAuthUserByCodeGrpcRequest.newBuilder()
                .setAuthUserId(request.authUserId().toString())
                .setVerificationCode(request.verificationCode())
                .build();
    }

    default VerifyAuthUserByPrivilegeRoleGrpcRequest toVerifyAuthUserByPrivilegeRoleGrpcRequest (VerifyAuthUserByPrivilegeRoleRequestDto request) {
        return VerifyAuthUserByPrivilegeRoleGrpcRequest.newBuilder()
                .setAuthUserId(request.authUserId().toString())
                .setRole(request.roles().name())
                .build();
    }
}
