package apigateway.client;

import apigateway.dto.auth.*;
import apigateway.dto.user.GetAuthUserByIdResponseDto;
import apigateway.dto.user.GetRoleByAuthUserIdRequestDto;
import apigateway.mapper.dto.AuthDtoMapper;
import apigateway.mapper.grpc.AuthGrpcMapper;
import auth.contract.v1.*;
import com.google.protobuf.Empty;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class AuthGrpcClient {
  private final AuthRpcServiceGrpc.AuthRpcServiceBlockingStub stub;
  private final AuthGrpcMapper grpcMapper;
  private final AuthDtoMapper dtoMapper;

  public AuthGrpcClient(
      AuthRpcServiceGrpc.AuthRpcServiceBlockingStub stub,
      AuthGrpcMapper grpcMapper,
      AuthDtoMapper dtoMapper) {
    this.stub = stub;
    this.grpcMapper = grpcMapper;
    this.dtoMapper = dtoMapper;
  }

  public String getAuthHealth() {
    GetAuthHealthGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getAuthHealth(Empty.getDefaultInstance());
    return response.getMessage();
  }

  public void signup(SignupRequestDto request) {
    SignupAuthGrpcRequest grpcRequest = grpcMapper.toSignupAuthGrpcRequest(request);
    stub.withDeadlineAfter(2, TimeUnit.SECONDS).signup(grpcRequest);
  }

  public LoginResponseDto login(LoginRequestDto request) {
    LoginAuthGrpcRequest grpcRequest = grpcMapper.toLoginAuthGrpcRequest(request);
    return dtoMapper.toLoginResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).login(grpcRequest));
  }

  public void logout(LogoutRequestDto request) {
    LogoutAuthGrpcRequest grpcRequest = grpcMapper.toLogoutAuthGrpcRequest(request);
    stub.withDeadlineAfter(2, TimeUnit.SECONDS).logout(grpcRequest);
  }

  public RefreshResponseDto refresh(RefreshRequestDto request) {
    RefreshAuthGrpcRequest grpcRequest = grpcMapper.toRefreshAuthGrpcRequest(request);
    return dtoMapper.toRefreshResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).refresh(grpcRequest));
  }

  public ChangePasswordResponseDto changePassword(
      ChangePasswordRequestDto request, UUID authUserId) {
    ChangePasswordGrpcRequest grpcRequest =
        grpcMapper.toChangePasswordGrpcRequest(request, authUserId);

    return dtoMapper.toChangePasswordResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).changePassword(grpcRequest));
  }

  public void blockUser(BlockAuthUserRequestDto request) {
    BlockAuthGrpcRequest grpcRequest = grpcMapper.toBlockUserGrpcRequest(request);
    stub.withDeadlineAfter(2, TimeUnit.SECONDS).blockAuthUser(grpcRequest);
  }

  public void unlockUser(UnlockAuthUserRequestDto request) {
    UnlockAuthGrpcRequest grpcRequest = grpcMapper.toUnlockUserGrpcRequest(request);
    stub.withDeadlineAfter(2, TimeUnit.SECONDS).unlockAuthUser(grpcRequest);
  }

  public VerifyAuthUserByCodeResponseDto verifyByCode(VerifyAuthUserByCodeRequestDto request) {
    VerifyAuthUserByCodeGrpcRequest grpcRequest =
        grpcMapper.toVerifyAuthUserByCodeGrpcRequest(request);
    return dtoMapper.toVerifyAuthUserByCodeResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).verifyAuthUserByCode(grpcRequest));
  }

  public void verifyByPrivilegedRole(VerifyAuthUserByPrivilegeRoleRequestDto request) {
    VerifyAuthUserByPrivilegeRoleGrpcRequest grpcRequest =
        grpcMapper.toVerifyAuthUserByPrivilegeRoleGrpcRequest(request);
    stub.withDeadlineAfter(2, TimeUnit.SECONDS).verifyAuthUserByPrivilegeRole(grpcRequest);
  }

  public void changeAuthUserRole(ChangeAuthUserRoleRequestDto request) {
    ChangeAuthUserRoleGrpcRequest grpcRequest = grpcMapper.toChangeAuthUserRoleGrpcRequest(request);
    stub.withDeadlineAfter(2, TimeUnit.SECONDS).changeAuthUserRole(grpcRequest);
  }

  public GetAuthUserByIdResponseDto getAuthUserById(GetRoleByAuthUserIdRequestDto request) {
    GetAuthUserByIdGrpcRequest grpcRequest = grpcMapper.toGetAuthUserByIdGrpcRequest(request);
    return dtoMapper.toGetAuthUserByIdResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getAuthUserById(grpcRequest));
  }

  public void forgetPassword(ForgetPasswordRequestDto request) {
    ForgetPasswordGrpcRequest grpcRequest = grpcMapper.toForgetPasswordGrpcRequest(request);
    stub.withDeadlineAfter(2, TimeUnit.SECONDS).forgetPassword(grpcRequest);
  }

  public ResetPasswordsResponseDto resetPassword(ResetPasswordRequestDto request) {
    ResetPasswordGrpcRequest grpcRequest = grpcMapper.toResetPasswordGrpcRequest(request);
    return dtoMapper.toResetPasswordsResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).resetPassword(grpcRequest));
  }

  public LoginResponseDto socialLogin(SocialLoginRequestDto request) {
    SocialLoginGrpcRequest grpcRequest = grpcMapper.toSocialLoginGrpcRequest(request);
    return dtoMapper.toLoginResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).socialLogin(grpcRequest));
  }
}
