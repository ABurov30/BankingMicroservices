package apigateway.client;

import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import transaction.contract.v1.*;

@Service
public class TransactionGrpcClient {
  private final TransactionRpcServiceGrpc.TransactionRpcServiceBlockingStub stub;

  public TransactionGrpcClient(TransactionRpcServiceGrpc.TransactionRpcServiceBlockingStub stub) {
    this.stub = stub;
  }

  public String getTransactionHealth() {
    GetTransactionHealthGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
            .getTransactionHealth(GetTransactionHealthGrpcRequest.newBuilder().build());
    return response.getMessage();
  }
}
