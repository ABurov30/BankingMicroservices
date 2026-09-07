package apigateway.client;

import apigateway.dto.request.user.GetRecipientRequestDto;
import apigateway.dto.request.user.GetUserInfoRequestDto;
import apigateway.dto.response.user.GetUserInfoResponseDto;
import apigateway.dto.result.user.GetRecipientResultDto;
import apigateway.mapper.grpc.UserGrpcMapper;
import apigateway.mapper.result.UserResultMapper;
import com.google.protobuf.Empty;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import user.contract.v1.*;

@Service
public class UserGrpcClient {
  private final UserRpcServiceGrpc.UserRpcServiceBlockingStub stub;
  private final UserGrpcMapper grpcMapper;
  private final UserResultMapper dtoMapper;

  public UserGrpcClient(
      UserRpcServiceGrpc.UserRpcServiceBlockingStub stub,
      UserGrpcMapper grpcMapper,
      UserResultMapper dtoMapper) {
    this.stub = stub;
    this.grpcMapper = grpcMapper;
    this.dtoMapper = dtoMapper;
  }

  public GetUserInfoResponseDto getUserInfo(GetUserInfoRequestDto getUserInfoRequest) {
    GetUserInfoGrpcRequest getUserInfoGrpcRequest =
        grpcMapper.toGetUserInfoGrpcRequest(getUserInfoRequest);
    return dtoMapper.toGetInfoResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getUserInfo(getUserInfoGrpcRequest));
  }

  public List<GetUserInfoResponseDto> getAllUserInfo() {
    return dtoMapper.toGetInfoResponseDtoList(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getAllUserInfo(Empty.getDefaultInstance()));
  }

  public GetRecipientResultDto getRecipientByEmail(GetRecipientRequestDto request) {
    return dtoMapper.toGetRecipientResultDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
            .getRecipientByEmail(grpcMapper.toGetRecipientByEmailRequest(request)));
  }

  public String getUserHealth() {
    GetUserHealthGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getUserHealth(Empty.getDefaultInstance());
    return response.getMessage();
  }
}
