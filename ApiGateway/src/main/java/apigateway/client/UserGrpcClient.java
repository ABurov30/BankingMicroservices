package apigateway.client;

import apigateway.dto.user.GetUserInfoRequestDto;
import apigateway.dto.user.GetUserInfoResponseDto;
import apigateway.mapper.UserMapper;
import com.google.protobuf.Empty;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import user.contract.v1.*;

@Service
public class UserGrpcClient {
  private final UserRpcServiceGrpc.UserRpcServiceBlockingStub stub;
  private final UserMapper userMapper;

  public UserGrpcClient(UserRpcServiceGrpc.UserRpcServiceBlockingStub stub, UserMapper userMapper) {
      this.stub = stub;
      this.userMapper = userMapper;
  }

  public GetUserInfoResponseDto getUserInfo(GetUserInfoRequestDto getUserInfoRequest) {
    GetUserInfoGrpcRequest getUserInfoGrpcRequest = userMapper.toGetUserInfoGrpcRequest(getUserInfoRequest);
    return userMapper.toGetInfoResponseDto(stub.withDeadlineAfter(2, TimeUnit.SECONDS).getUserInfo(getUserInfoGrpcRequest));
  }

  public String getUserHealth() {
    GetUserHealthGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getUserHealth(Empty.getDefaultInstance());
    return response.getMessage();
  }
}
