package apigateway.client;

import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import transaction.contract.v1.GetTransactionRequest;
import transaction.contract.v1.GetTransactionResponse;
import transaction.contract.v1.TransactionRpcServiceGrpc;

@Service
public class TransactionGrpcClient {
  private final TransactionRpcServiceGrpc.TransactionRpcServiceBlockingStub stub;

  public TransactionGrpcClient(TransactionRpcServiceGrpc.TransactionRpcServiceBlockingStub stub) {
    this.stub = stub;
  }

  public String getTransaction() {
    GetTransactionResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
            .getTransaction(GetTransactionRequest.newBuilder().build());
    return response.getMessage();
  }
}
