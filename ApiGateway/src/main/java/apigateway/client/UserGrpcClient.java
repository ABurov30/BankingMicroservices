package apigateway.client;

import apigateway.dto.request.user.GetRecipientRequestDto;
import apigateway.dto.request.user.GetUserInfoRequestDto;
import apigateway.dto.response.user.GetUserInfoResponseDto;
import apigateway.dto.result.user.GetRecipientResultDto;
import apigateway.mapper.grpc.UserGrpcMapper;
import apigateway.mapper.result.UserResultMapper;
import com.google.protobuf.Empty;
import grpcfutureadapter.GrpcFutureAdapter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import user.contract.v1.*;

@Service
@RequiredArgsConstructor
public class UserGrpcClient {
  private final UserRpcServiceGrpc.UserRpcServiceBlockingStub stub;
  private final UserRpcServiceGrpc.UserRpcServiceFutureStub futureStub;
  private final UserGrpcMapper grpcMapper;
  private final UserResultMapper dtoMapper;

  public GetUserInfoResponseDto getUserInfo(GetUserInfoRequestDto getUserInfoRequest) {
    GetUserInfoGrpcRequest getUserInfoGrpcRequest =
        grpcMapper.toGetUserInfoGrpcRequest(getUserInfoRequest);
    return dtoMapper.toGetInfoResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getUserInfo(getUserInfoGrpcRequest));
  }

  public CompletableFuture<GetUserInfoResponseDto> getUserInfoAsync(
      GetUserInfoRequestDto getUserInfoRequest) {
    GetUserInfoGrpcRequest getUserInfoGrpcRequest =
        grpcMapper.toGetUserInfoGrpcRequest(getUserInfoRequest);
    return GrpcFutureAdapter.toCompletableFuture(
            futureStub.withDeadlineAfter(2, TimeUnit.SECONDS).getUserInfo(getUserInfoGrpcRequest))
        .thenApply(dtoMapper::toGetInfoResponseDto);
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
