package apigateway.client;

import apigateway.dto.auth.*;
import apigateway.mapper.AuthMapper;
import auth.contract.v1.*;
import com.google.protobuf.Empty;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

@Service
public class AuthGrpcClient {
    private final AuthRpcServiceGrpc.AuthRpcServiceBlockingStub stub;
    private final AuthMapper authMapper;

    public AuthGrpcClient(AuthRpcServiceGrpc.AuthRpcServiceBlockingStub stub, AuthMapper authMapper) {
        this.stub = stub;
        this.authMapper = authMapper;
    }

    public String getAuthHealth() {
        GetAuthHealthGrpcResponse response =
                stub.withDeadlineAfter(2, TimeUnit.SECONDS).getAuthHealth(Empty.getDefaultInstance());
        return response.getMessage();
    }

    public void signup(SignupRequestDto request) {
        SignupAuthGrpcRequest grpcRequest = authMapper.toSignupAuthGrpcRequest(request);
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).signup(grpcRequest);
    }

    public LoginResponseDto login(LoginRequestDto request) {
        LoginAuthGrpcRequest grpcRequest = authMapper.toLoginAuthGrpcRequest(request);
        return authMapper.toLoginResponseDto(stub.withDeadlineAfter(2, TimeUnit.SECONDS).login(grpcRequest));
    }

    public void logout (LogoutRequestDto request) {
        LogoutAuthGrpcRequest grpcRequest = authMapper.toLogoutAuthGrpcRequest(request);
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).logout(grpcRequest);
    }

    public RefreshResponseDto refresh (RefreshRequestDto request) {
        RefreshAuthGrpcRequest grpcRequest = authMapper.toRefreshAuthGrpcRequest(request);
        return authMapper.toRefreshResponseDto(stub.withDeadlineAfter(2, TimeUnit.SECONDS).refresh(grpcRequest));
    }

    public void changePassword (ChangePasswordRequestDto request) {
        ChangePasswordGrpcRequest grpcRequest = authMapper.toChangePasswordGrpcRequest(request);
        stub.withDeadlineAfter(2,TimeUnit.SECONDS).changePassword(grpcRequest);
    }

    public void blockUser (BlockAuthUserRequestDto request) {
        BlockAuthGrpcRequest grpcRequest = authMapper.toBlockUserGrpcRequest(request);
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).blockAuthUser(grpcRequest);
    }

    public void unlockUser (UnlockAuthUserRequestDto request) {
        UnlockAuthGrpcRequest grpcRequest = authMapper.toUnlockUserGrpcRequest(request);
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).unlockAuthUser(grpcRequest);
    }

    public VerifyAuthUserByCodeResponseDto verifyByCode(VerifyAuthUserByCodeRequestDto request) {
        VerifyAuthUserByCodeGrpcRequest grpcRequest = authMapper.toVerifyAuthUserByCodeGrpcRequest(request);
        return authMapper.toVerifyAuthUserByCodeResponseDto(stub.withDeadlineAfter(2, TimeUnit.SECONDS).verifyAuthUserByCode(grpcRequest));
    }

    public void verifyByPrivilegedRole(VerifyAuthUserByPrivilegeRoleRequestDto request) {
        VerifyAuthUserByPrivilegeRoleGrpcRequest grpcRequest = authMapper.toVerifyAuthUserByPrivilegeRoleGrpcRequest(request);
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).verifyAuthUserByPrivilegeRole(grpcRequest);
    }

    public void changeAuthUserRole (ChangeAuthUserRoleRequestDto request) {
        ChangeAuthUserRoleGrpcRequest grpcRequest = authMapper.toChangeAuthUserRoleGrpcRequest(request);
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).changeAuthUserRole(grpcRequest);
    }
}
