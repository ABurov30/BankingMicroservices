package apigateway.client;

import apigateway.dto.card.CreateCardRequestDto;
import apigateway.dto.card.CreateCardResponseDto;
import apigateway.dto.card.GetCardByAccountIdResponseDto;
import apigateway.dto.card.UpdateCardRequestDto;
import apigateway.dto.card.UpdateCardResponseDto;
import apigateway.mapper.dto.CardDtoMapper;
import apigateway.mapper.grpc.CardGrpcMapper;
import card.contract.v1.*;
import com.google.protobuf.Empty;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class CardGrpcClient {
  private final CardRpcServiceGrpc.CardRpcServiceBlockingStub stub;
  private final CardGrpcMapper grpcMapper;
  private final CardDtoMapper dtoMapper;

  public CardGrpcClient(
      CardRpcServiceGrpc.CardRpcServiceBlockingStub stub,
      CardGrpcMapper grpcMapper,
      CardDtoMapper dtoMapper) {
    this.stub = stub;
    this.grpcMapper = grpcMapper;
    this.dtoMapper = dtoMapper;
  }

  public String getCardHealth() {
    GetCardHealthGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getCardHealth(Empty.getDefaultInstance());
    return response.getMessage();
  }

  public CreateCardResponseDto createCard(
      CreateCardRequestDto request, UUID authUserId, String role) {
    CreateCardGrpcRequest grpcRequest =
        grpcMapper.toCreateCardGrpcRequest(request, authUserId, role);
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

  public List<GetCardByAccountIdResponseDto> getCardsByAccountId(UUID accountId) {
    GetCardByAccountIdGrpcRequest request =
        GetCardByAccountIdGrpcRequest.newBuilder().setAccountId(accountId.toString()).build();
    GetCardsGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getCardsByAccountId(request);

    return response.getCardsList().stream()
        .map(dtoMapper::toGetCardByAccountIdResponseDto)
        .toList();
  }
}
