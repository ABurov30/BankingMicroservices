package apigateway.client;

import apigateway.dto.user.GetUserInfoByEmailRequestDto;
import apigateway.dto.user.GetUserInfoRequestDto;
import apigateway.dto.user.GetUserInfoResponseDto;
import apigateway.mapper.dto.UserDtoMapper;
import apigateway.mapper.grpc.UserGrpcMapper;
import com.google.protobuf.Empty;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import user.contract.v1.*;

@Service
public class UserGrpcClient {
  private final UserRpcServiceGrpc.UserRpcServiceBlockingStub stub;
  private final UserGrpcMapper grpcMapper;
  private final UserDtoMapper dtoMapper;

  public UserGrpcClient(UserRpcServiceGrpc.UserRpcServiceBlockingStub stub, UserGrpcMapper grpcMapper, UserDtoMapper dtoMapper) {
      this.stub = stub; this.grpcMapper = grpcMapper; this.dtoMapper = dtoMapper;
  }

  public GetUserInfoResponseDto getUserInfo(GetUserInfoRequestDto getUserInfoRequest) {
    GetUserInfoGrpcRequest getUserInfoGrpcRequest = grpcMapper.toGetUserInfoGrpcRequest(getUserInfoRequest);
    return dtoMapper.toGetInfoResponseDto(stub.withDeadlineAfter(2, TimeUnit.SECONDS).getUserInfo(getUserInfoGrpcRequest));
  }

  public List<GetUserInfoResponseDto> getAllUserInfo() {
      return dtoMapper.toGetInfoResponseDtoList(
          stub.withDeadlineAfter(2, TimeUnit.SECONDS).getAllUserInfo(Empty.getDefaultInstance())
      );
  }

  public GetUserInfoResponseDto getUserInfoByEmail (GetUserInfoByEmailRequestDto request) {
      return dtoMapper.toGetInfoResponseDto(
              stub.withDeadlineAfter(2, TimeUnit.SECONDS).getUserInfoByEmail(grpcMapper.toGetUserInfoByEmailRequest(request))
      );
  };

  public String getUserHealth() {
    GetUserHealthGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getUserHealth(Empty.getDefaultInstance());
    return response.getMessage();
  }
}
