package userservice.grpc;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import user.contract.v1.*;
import userservice.dto.GetUserInfoCommand;
import userservice.dto.GetUserInfoResult;
import userservice.mapper.UserMapper;
import userservice.service.UserService;

import java.time.LocalDateTime;

@Service
public class UserGrpcService extends UserRpcServiceGrpc.UserRpcServiceImplBase {
    private final UserService userService;
    private final UserMapper userMapper;

    public UserGrpcService(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @Override
    public void getUserHealth(Empty request, StreamObserver<GetUserHealthGrpcResponse> responseObserver) {
        GetUserHealthGrpcResponse response = GetUserHealthGrpcResponse.newBuilder().setMessage("User service GRPC health " + LocalDateTime.now()).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getUserInfo(GetUserInfoGrpcRequest request, StreamObserver<GetUserInfoGrpcResponse> responseObserver) {
        GetUserInfoCommand getUserInfoCommand = userMapper.toGetUserInfoCommand(request);
        GetUserInfoGrpcResponse response = userMapper.toGetUserInfoGrpcResponse(userService.getUserInfo(getUserInfoCommand));

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
