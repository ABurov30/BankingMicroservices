package apigateway.client;

import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import user.contract.v1.*;

@Service
public class UserGrpcClient {
  private final UserRpcServiceGrpc.UserRpcServiceBlockingStub stub;

  public UserGrpcClient(UserRpcServiceGrpc.UserRpcServiceBlockingStub stub) {
    this.stub = stub;
  }

  public String getUserHealth() {
    GetUserHealthGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getUserHealth(GetUserHealthGrpcRequest.newBuilder().build());
    return response.getMessage();
  }
}
