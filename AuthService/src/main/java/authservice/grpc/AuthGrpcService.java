package authservice.grpc;

import auth.contract.v1.*;
import authservice.dto.*;
import authservice.mapper.AuthMapper;
import authservice.service.AuthService;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthGrpcService extends AuthRpcServiceGrpc.AuthRpcServiceImplBase {
    private final AuthService authService;
    private final AuthMapper authMapper;

    public AuthGrpcService (AuthService authService, AuthMapper authMapper) {
        this.authService =authService;
        this.authMapper = authMapper;
    }

    @Override
    public void getAuthHealth(Empty request, StreamObserver<GetAuthHealthGrpcResponse> responseObserver) {
        GetAuthHealthGrpcResponse response = GetAuthHealthGrpcResponse.newBuilder().setMessage("Auth service GRPC health " + LocalDateTime.now()).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void signup(SignupAuthGrpcRequest request, StreamObserver<SignupAuthGrpcResponse> responseObserver) {
        SignupCommand signupCommand = authMapper.toSignupCommand(request);
        SignupAuthGrpcResponse response = authMapper.toSignupGrpcResponse(authService.signup(signupCommand));

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void login(LoginAuthGrpcRequest request, StreamObserver<LoginAuthGrpcResponse> responseObserver) {
        LoginCommand loginCommand = authMapper.toLoginCommand(request);
        LoginAuthGrpcResponse response = authMapper.toLoginGrpcResponse(authService.login(loginCommand));

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void logout(LogoutAuthGrpcRequest request, StreamObserver<Empty> responseObserver) {
        LogoutCommand logoutCommand = authMapper.toLogoutCommand(request);
        authService.logout(logoutCommand);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void refresh(RefreshAuthGrpcRequest request, StreamObserver<RefreshAuthGrpcResponse> responseObserver) {
        RefreshCommand refreshCommand = authMapper.toRefreshCommand(request);
        RefreshAuthGrpcResponse refreshAuthGrpcResponse = authMapper.toRefreshGrpcResponse(authService.refresh(refreshCommand));

        responseObserver.onNext(refreshAuthGrpcResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void changePassword (ChangePasswordGrpcRequest request, StreamObserver<Empty> responseObserver) {
        ChangePasswordCommand changePasswordCommand = authMapper.toChangePasswordCommand(request);
        authService.changePassword(changePasswordCommand);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void blockAuthUser (BlockAuthGrpcRequest request, StreamObserver<Empty> responseObserver) {
        BlockAuthUserCommand blockAuthUserCommand = authMapper.toBlockAuthUserCommand(request);
        authService.blockUser(blockAuthUserCommand);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void unlockAuthUser (UnlockAuthGrpcRequest request, StreamObserver<Empty> responseObserver) {
        UnlockAuthUserCommand unlockAuthUserCommand = authMapper.toUnlockAuthUserCommand(request);
        authService.unlockUser(unlockAuthUserCommand);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void verifyAuthUser(VerifyAuthGrpcRequest request, StreamObserver<Empty> responseObserver) {
        VerifyAuthUserCommand verifyAuthUserCommand = authMapper.toVerifyAuthUserCommand(request);
        authService.verifyUser(verifyAuthUserCommand);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
