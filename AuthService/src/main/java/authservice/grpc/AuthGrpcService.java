package authservice.grpc;

import auth.contract.v1.*;
import authservice.dto.*;
import authservice.mapper.command.AuthCommandMapper;
import authservice.mapper.grpc.AuthGrpcMapper;
import authservice.service.AuthService;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class AuthGrpcService extends AuthRpcServiceGrpc.AuthRpcServiceImplBase {
  private final AuthService authService;
  private final AuthCommandMapper commandMapper;
  private final AuthGrpcMapper grpcMapper;

  public AuthGrpcService(
      AuthService authService, AuthCommandMapper commandMapper, AuthGrpcMapper grpcMapper) {
    this.authService = authService;
    this.commandMapper = commandMapper;
    this.grpcMapper = grpcMapper;
  }

  @Override
  public void getAuthHealth(
      Empty request, StreamObserver<GetAuthHealthGrpcResponse> responseObserver) {
    GetAuthHealthGrpcResponse response =
        GetAuthHealthGrpcResponse.newBuilder()
            .setMessage("Auth service GRPC health " + LocalDateTime.now())
            .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void signup(SignupAuthGrpcRequest request, StreamObserver<Empty> responseObserver) {
    SignupCommand signupCommand = commandMapper.toSignupCommand(request);
    authService.signup(signupCommand);

    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void login(
      LoginAuthGrpcRequest request, StreamObserver<LoginAuthGrpcResponse> responseObserver) {
    LoginCommand loginCommand = commandMapper.toLoginCommand(request);
    LoginAuthGrpcResponse response =
        grpcMapper.toLoginGrpcResponse(authService.login(loginCommand));

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void logout(LogoutAuthGrpcRequest request, StreamObserver<Empty> responseObserver) {
    LogoutCommand logoutCommand = commandMapper.toLogoutCommand(request);
    authService.logout(logoutCommand);

    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void refresh(
      RefreshAuthGrpcRequest request, StreamObserver<RefreshAuthGrpcResponse> responseObserver) {
    RefreshCommand refreshCommand = commandMapper.toRefreshCommand(request);
    RefreshAuthGrpcResponse refreshAuthGrpcResponse =
        grpcMapper.toRefreshGrpcResponse(authService.refresh(refreshCommand));

    responseObserver.onNext(refreshAuthGrpcResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void changePassword(
      ChangePasswordGrpcRequest request, StreamObserver<Empty> responseObserver) {
    ChangePasswordCommand changePasswordCommand = commandMapper.toChangePasswordCommand(request);
    authService.changePassword(changePasswordCommand);

    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void blockAuthUser(BlockAuthGrpcRequest request, StreamObserver<Empty> responseObserver) {
    BlockAuthUserCommand blockAuthUserCommand = commandMapper.toBlockAuthUserCommand(request);
    authService.blockUser(blockAuthUserCommand);

    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void unlockAuthUser(
      UnlockAuthGrpcRequest request, StreamObserver<Empty> responseObserver) {
    UnlockAuthUserCommand unlockAuthUserCommand = commandMapper.toUnlockAuthUserCommand(request);
    authService.unlockUser(unlockAuthUserCommand);

    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void verifyAuthUserByPrivilegeRole(
      VerifyAuthUserByPrivilegeRoleGrpcRequest request, StreamObserver<Empty> responseObserver) {
    VerifyAuthUserByPrivilegeRoleCommand verifyAuthUserByPrivilegeRoleCommand =
        commandMapper.toVerifyAuthUserByPrivilegeRoleCommand(request);
    authService.verifyByPrivilegedRole(verifyAuthUserByPrivilegeRoleCommand);

    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void verifyAuthUserByCode(
      VerifyAuthUserByCodeGrpcRequest request,
      StreamObserver<VerifyAuthUserByCodeGrpcResponse> responseObserver) {
    VerifyAuthUserByCodeCommand verifyAuthUserByCodeCommand =
        commandMapper.toVerifyAuthUserByCodeCommand(request);
    VerifyAuthUserByCodeGrpcResponse response =
        grpcMapper.toVerifyAuthUserByCodeGrpcResponse(
            authService.verifyByCode(verifyAuthUserByCodeCommand));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void changeAuthUserRole(
      ChangeAuthUserRoleGrpcRequest request, StreamObserver<Empty> responseObserver) {
    ChangeAuthUserRoleCommand changeAuthUserRoleCommand =
        commandMapper.toChangeAuthUserRoleCommand(request);
    authService.changeAuthUserRole(changeAuthUserRoleCommand);

    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void getAuthUserById(
      GetAuthUserByIdGrpcRequest request,
      StreamObserver<GetAuthUserByIdGrpcResponse> responseObserver) {
    GetAuthUserByIdCommand command = commandMapper.toGetAuthUserByIdCommand(request);
    GetAuthUserByIdGrpcResponse response =
        grpcMapper.toGetAuthUserByIdGrpcResponse(authService.getAuthUserById(command));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void forgetPassword(
      ForgetPasswordGrpcRequest request, StreamObserver<Empty> responseObserver) {
    ForgetPasswordCommand forgetPasswordCommand = commandMapper.toForgetPasswordCommand(request);
    authService.forgetPassword(forgetPasswordCommand);
    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void resetPassword(
      ResetPasswordGrpcRequest request,
      StreamObserver<ResetPasswordsGrpcResponse> responseObserver) {
    ResetPasswordCommand command = commandMapper.toResetPasswordCommand(request);
    ResetPasswordsGrpcResponse response =
        grpcMapper.toResetPasswordsGrpcResponse(authService.resetPassword(command));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
