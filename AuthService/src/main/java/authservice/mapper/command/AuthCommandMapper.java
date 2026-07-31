package authservice.mapper.command;

import auth.contract.v1.*;
import authservice.dto.*;
import enums.auth.Roles;
import org.mapstruct.Mapper;

import java.util.Locale;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AuthCommandMapper {
    default SignupCommand toSignupCommand(SignupAuthGrpcRequest request) {
        return new SignupCommand(request.getEmail().trim().toLowerCase(Locale.ROOT), request.getPassword(), request.getFirstName(), request.getLastName());
    }

    default LoginCommand toLoginCommand(LoginAuthGrpcRequest request) {
        return new LoginCommand(request.getEmail().trim().toLowerCase(Locale.ROOT), request.getPassword());
    }

    default LogoutCommand toLogoutCommand(LogoutAuthGrpcRequest request) {
        return new LogoutCommand(request.getRefreshToken());
    }

    default RefreshCommand toRefreshCommand(RefreshAuthGrpcRequest request) {
        return new RefreshCommand(request.getRefreshToken());
    }

    default ChangePasswordCommand toChangePasswordCommand(ChangePasswordGrpcRequest request) {
        return new ChangePasswordCommand(UUID.fromString(request.getAuthUserId()), request.getOldPassword(), request.getNewPassword());
    }

    default BlockAuthUserCommand toBlockAuthUserCommand(BlockAuthGrpcRequest request) {
        return new BlockAuthUserCommand(UUID.fromString(request.getAuthUserId()));
    }

    default UnlockAuthUserCommand toUnlockAuthUserCommand(UnlockAuthGrpcRequest request) {
        return new UnlockAuthUserCommand(UUID.fromString(request.getAuthUserId()));
    }

    default VerifyAuthUserByCodeCommand toVerifyAuthUserByCodeCommand(VerifyAuthUserByCodeGrpcRequest request) {
        return new VerifyAuthUserByCodeCommand(UUID.fromString(request.getAuthUserId()), request.getVerificationCode());
    }

    default VerifyAuthUserByPrivilegeRoleCommand toVerifyAuthUserByPrivilegeRoleCommand(VerifyAuthUserByPrivilegeRoleGrpcRequest request) {
        return new VerifyAuthUserByPrivilegeRoleCommand(UUID.fromString(request.getAuthUserId()), Roles.valueOf(request.getRole()));
    }

    default ChangeAuthUserRoleCommand toChangeAuthUserRoleCommand(ChangeAuthUserRoleGrpcRequest request) {
        return new ChangeAuthUserRoleCommand(UUID.fromString(request.getAuthUserId()), Roles.valueOf(request.getRole()));
    }

    default GetRoleByAuthUserIdCommand toGetRoleByAuthUserIdCommand(GetRoleByAuthUserIdGrpcRequest request) {
        return new GetRoleByAuthUserIdCommand(UUID.fromString(request.getAuthUserId()));
    }

    default ForgetPasswordCommand toForgetPasswordCommand(ForgetPasswordGrpcRequest request) {
        return new ForgetPasswordCommand(request.getEmail());
    }

    default ResetPasswordCommand toResetPasswordCommand(ResetPasswordGrpcRequest request) {
        return new ResetPasswordCommand(UUID.fromString(request.getAuthUserId()), request.getNewPassword());
    }
}
