package apigateway.client;

import apigateway.dto.command.card.GetCardsByAccountIdCommandDto;
import apigateway.dto.request.card.CreateCardRequestDto;
import apigateway.dto.request.card.UpdateCardRequestDto;
import apigateway.dto.response.account.GetAccountResponseDto;
import apigateway.dto.response.card.CreateCardResponseDto;
import apigateway.dto.response.card.GetCardByAccountIdResponseDto;
import apigateway.dto.response.card.UpdateCardResponseDto;
import apigateway.mapper.grpc.CardGrpcMapper;
import apigateway.mapper.result.CardResultMapper;
import card.contract.v1.*;
import com.google.protobuf.Empty;
import grpcfutureadapter.GrpcFutureAdapter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardGrpcClient {
  private final CardRpcServiceGrpc.CardRpcServiceBlockingStub stub;
  private final CardRpcServiceGrpc.CardRpcServiceFutureStub futureStub;
  private final CardGrpcMapper grpcMapper;
  private final CardResultMapper dtoMapper;

  public String getCardHealth() {
    GetCardHealthGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getCardHealth(Empty.getDefaultInstance());
    return response.getMessage();
  }

  public CreateCardResponseDto createCard(
      CreateCardRequestDto request, UUID authUserId, String role, GetAccountResponseDto account) {
    CreateCardGrpcRequest grpcRequest =
        grpcMapper.toCreateCardGrpcRequest(request, authUserId, role, account);
    return dtoMapper.toCreateCardResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).createCard(grpcRequest));
  }

  public UpdateCardResponseDto updateCard(
      UpdateCardRequestDto request, UUID authUserId, String role) {
    UpdateCardGrpcRequest grpcRequest =
        grpcMapper.toUpdateCardGrpcRequest(request, authUserId, role);
    return dtoMapper.toUpdateCardResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).updateCard(grpcRequest));
  }

  public List<GetCardByAccountIdResponseDto> getCardsByAccountId(
      GetCardsByAccountIdCommandDto command) {
    GetCardByAccountIdGrpcRequest request = grpcMapper.toGetCardByAccountIdGrpcRequest(command);
    GetCardsGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getCardsByAccountId(request);

    return response.getCardsList().stream()
        .map(dtoMapper::toGetCardByAccountIdResponseDto)
        .toList();
  }

  public CompletableFuture<List<GetCardByAccountIdResponseDto>> getCardsByAccountIdAsync(
      GetCardsByAccountIdCommandDto command) {

    GetCardByAccountIdGrpcRequest request = grpcMapper.toGetCardByAccountIdGrpcRequest(command);

    return GrpcFutureAdapter.toCompletableFuture(
            futureStub.withDeadlineAfter(2, TimeUnit.SECONDS).getCardsByAccountId(request))
        .thenApply(
            response ->
                response.getCardsList().stream()
                    .map(dtoMapper::toGetCardByAccountIdResponseDto)
                    .toList());
  }
}
