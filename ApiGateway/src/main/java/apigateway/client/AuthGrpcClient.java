package apigateway.client;

import apigateway.dto.*;
import apigateway.mapper.AuthMapper;
import auth.contract.v1.*;

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

    public String getAuth() {
        GetAuthGrpcResponse response =
                stub.withDeadlineAfter(2, TimeUnit.SECONDS).getAuth(GetAuthGrpcRequest.newBuilder().build());
        return response.getMessage();
    }

    public SignupResponseDto signup(SignupRequestDto request) {
        SignupAuthGrpcRequest grpcRequest = authMapper.toSignupAuthGrpcRequest(request);
        return authMapper.toSignupResponseDto(stub.withDeadlineAfter(2, TimeUnit.SECONDS).signup(grpcRequest));
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
}
