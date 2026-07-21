package apigateway.client;

import apigateway.dto.card.CreateCardRequestDto;
import apigateway.dto.card.CreateCardResponseDto;
import apigateway.mapper.CardMapper;
import card.contract.v1.*;
import com.google.protobuf.Empty;

import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class CardGrpcClient {
  private final CardRpcServiceGrpc.CardRpcServiceBlockingStub stub;
  private final CardMapper cardMapper;

  public CardGrpcClient(
          CardRpcServiceGrpc.CardRpcServiceBlockingStub stub,
          CardMapper cardMapper
  ) {
    this.stub = stub;
    this.cardMapper = cardMapper;
  }

  public String getCardHealth() {
    GetCardHealthGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getCardHealth(Empty.getDefaultInstance());
    return response.getMessage();
  }

  public CreateCardResponseDto createCard (CreateCardRequestDto request) {
      CreateCardGrpcRequest grpcRequest = cardMapper.toCreateCardGrpcRequest(request);
      return cardMapper.toCreateCardResponseDto(stub.withDeadlineAfter(2, TimeUnit.SECONDS).createCard(grpcRequest));
  }
}
