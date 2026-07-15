package apigateway.client;

import account.contract.v1.*;

import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class AccountGrpcClient {
  private final AccountRpcServiceGrpc.AccountRpcServiceBlockingStub stub;

  public AccountGrpcClient(AccountRpcServiceGrpc.AccountRpcServiceBlockingStub stub) {
    this.stub = stub;
  }

  public String getAccountHealth() {
    GetAccountHealthGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
            .getAccountHealth(GetAccountHealthGrpcRequest.newBuilder().build());
    return response.getMessage();
  }
}
