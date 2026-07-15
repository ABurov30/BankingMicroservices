package apigateway.client;

import card.contract.v1.*;

import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class CardGrpcClient {
  private final CardRpcServiceGrpc.CardRpcServiceBlockingStub stub;

  public CardGrpcClient(CardRpcServiceGrpc.CardRpcServiceBlockingStub stub) {
    this.stub = stub;
  }

  public String getCardHealth() {
    GetCardHealthGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getCardHealth(GetCardHealthGrpcRequest.newBuilder().build());
    return response.getMessage();
  }
}
