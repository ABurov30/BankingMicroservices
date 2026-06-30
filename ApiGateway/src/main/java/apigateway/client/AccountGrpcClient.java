package apigateway.client;

import account.contract.v1.AccountRpcServiceGrpc;
import account.contract.v1.GetAccountRequest;
import account.contract.v1.GetAccountResponse;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class AccountGrpcClient {
  private final AccountRpcServiceGrpc.AccountRpcServiceBlockingStub stub;

  public AccountGrpcClient(AccountRpcServiceGrpc.AccountRpcServiceBlockingStub stub) {
    this.stub = stub;
  }

  public String getAccount() {
    GetAccountResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
            .getAccount(GetAccountRequest.newBuilder().build());
    return response.getMessage();
  }
}
