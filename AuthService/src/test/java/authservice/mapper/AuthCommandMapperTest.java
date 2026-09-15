package authservice.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import auth.contract.v1.*;
import authservice.mapper.command.AuthCommandMapperImpl;
import enums.auth.Roles;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthCommandMapperTest {
  @Test
  void mapsGrpcCommandsAndNormalizesEmails() {
    UUID id = UUID.randomUUID();
    var mapper = new AuthCommandMapperImpl();
    assertThat(
            mapper
                .toSignupCommand(
                    SignupAuthGrpcRequest.newBuilder()
                        .setEmail(" USER@Test ")
                        .setPassword("p")
                        .setFirstName("F")
                        .setLastName("L")
                        .build())
                .email())
        .isEqualTo("user@test");
    assertThat(
            mapper
                .toLoginCommand(
                    LoginAuthGrpcRequest.newBuilder()
                        .setEmail(" USER@Test ")
                        .setPassword("p")
                        .build())
                .email())
        .isEqualTo("user@test");
    assertThat(
            mapper
                .toGetAuthUserByIdsCommand(
                    GetAuthUserByIdsGrpcRequest.newBuilder().addAuthUserId(id.toString()).build())
                .authUserIds())
        .containsExactly(id);
    assertThat(
            mapper
                .toLogoutCommand(LogoutAuthGrpcRequest.newBuilder().setRefreshToken("r").build())
                .refreshToken())
        .isEqualTo("r");
    assertThat(
            mapper
                .toRefreshCommand(RefreshAuthGrpcRequest.newBuilder().setRefreshToken("r").build())
                .refreshToken())
        .isEqualTo("r");
    assertThat(
            mapper
                .toChangePasswordCommand(
                    ChangePasswordGrpcRequest.newBuilder()
                        .setAuthUserId(id.toString())
                        .setOldPassword("o")
                        .setNewPassword("n")
                        .build())
                .authUserId())
        .isEqualTo(id);
    assertThat(
            mapper
                .toBlockAuthUserCommand(
                    BlockAuthGrpcRequest.newBuilder().setAuthUserId(id.toString()).build())
                .authUserId())
        .isEqualTo(id);
    assertThat(
            mapper
                .toUnlockAuthUserCommand(
                    UnlockAuthGrpcRequest.newBuilder().setAuthUserId(id.toString()).build())
                .authUserId())
        .isEqualTo(id);
    assertThat(
            mapper
                .toVerifyAuthUserByCodeCommand(
                    VerifyAuthUserByCodeGrpcRequest.newBuilder()
                        .setAuthUserId(id.toString())
                        .setVerificationCode("c")
                        .build())
                .authUserId())
        .isEqualTo(id);
    assertThat(
            mapper
                .toVerifyAuthUserByPrivilegeRoleCommand(
                    VerifyAuthUserByPrivilegeRoleGrpcRequest.newBuilder()
                        .setAuthUserId(id.toString())
                        .setRole(Roles.ADMIN.name())
                        .build())
                .role())
        .isEqualTo(Roles.ADMIN);
    assertThat(
            mapper
                .toChangeAuthUserRoleCommand(
                    ChangeAuthUserRoleGrpcRequest.newBuilder()
                        .setAuthUserId(id.toString())
                        .setRole(Roles.MANAGER.name())
                        .build())
                .role())
        .isEqualTo(Roles.MANAGER);
    assertThat(
            mapper
                .toGetAuthUserByIdCommand(
                    GetAuthUserByIdGrpcRequest.newBuilder().setAuthUserId(id.toString()).build())
                .authUserId())
        .isEqualTo(id);
    assertThat(
            mapper
                .toForgetPasswordCommand(
                    ForgetPasswordGrpcRequest.newBuilder().setEmail("u@test").build())
                .email())
        .isEqualTo("u@test");
    assertThat(
            mapper
                .toResetPasswordCommand(
                    ResetPasswordGrpcRequest.newBuilder()
                        .setResetPasswordToken("t")
                        .setNewPassword("n")
                        .build())
                .newPassword())
        .isEqualTo("n");
  }
}
