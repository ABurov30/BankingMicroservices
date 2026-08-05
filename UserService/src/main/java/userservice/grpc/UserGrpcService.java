package userservice.grpc;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import user.contract.v1.*;
import userservice.dto.GetUserInfoByEmailCommand;
import userservice.dto.GetUserInfoCommand;
import userservice.dto.GetUserInfoResult;
import userservice.mapper.command.UserCommandMapper;
import userservice.mapper.grpc.UserGrpcMapper;
import userservice.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserGrpcService extends UserRpcServiceGrpc.UserRpcServiceImplBase {
    private final UserService userService;
    private final UserCommandMapper commandMapper;
    private final UserGrpcMapper grpcMapper;

    public UserGrpcService(UserService userService, UserCommandMapper commandMapper, UserGrpcMapper grpcMapper) {
        this.userService = userService;
        this.commandMapper = commandMapper;
        this.grpcMapper = grpcMapper;
    }

    @Override
    public void getUserHealth(Empty request, StreamObserver<GetUserHealthGrpcResponse> responseObserver) {
        GetUserHealthGrpcResponse response = GetUserHealthGrpcResponse.newBuilder().setMessage("User service GRPC health " + LocalDateTime.now()).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getUserInfo(GetUserInfoGrpcRequest request, StreamObserver<GetUserInfoGrpcResponse> responseObserver) {
        GetUserInfoCommand getUserInfoCommand = commandMapper.toGetUserInfoCommand(request);
        GetUserInfoGrpcResponse response = grpcMapper.toGetUserInfoGrpcResponse(userService.getUserInfo(getUserInfoCommand));

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getAllUserInfo (Empty request, StreamObserver<GetAllUserInfoGrpcResponse> responseObserver) {
        List<GetUserInfoResult> resultList = userService.getAllUserInfo();
        List<UserResponse> responses = resultList.stream().map(grpcMapper::toUserResponse).toList();

        responseObserver.onNext(grpcMapper.toGetAllUserInfoGrpcResponse(responses));
        responseObserver.onCompleted();
    }

    @Override
    public void getUserInfoByEmail (GetUserInfoByEmailRequest request, StreamObserver<GetUserInfoGrpcResponse> responseObserver) {
        GetUserInfoByEmailCommand getUserInfoCommand = commandMapper.toGetUserInfoByEmailCommand(request);
        GetUserInfoGrpcResponse response = grpcMapper.toGetUserInfoGrpcResponse(userService.getUserInfoByEmail(getUserInfoCommand));

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
