package apigateway.client;

import card.contract.v1.CardRpcServiceGrpc;
import card.contract.v1.GetCardRequest;
import card.contract.v1.GetCardResponse;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class CardGrpcClient {
  private final CardRpcServiceGrpc.CardRpcServiceBlockingStub stub;

  public CardGrpcClient(CardRpcServiceGrpc.CardRpcServiceBlockingStub stub) {
    this.stub = stub;
  }

  public String getCard() {
    GetCardResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getCard(GetCardRequest.newBuilder().build());
    return response.getMessage();
  }
}
