package apigateway.client;

import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import user.contract.v1.GetUserRequest;
import user.contract.v1.GetUserResponse;
import user.contract.v1.UserRpcServiceGrpc;

@Service
public class UserGrpcClient {
  private final UserRpcServiceGrpc.UserRpcServiceBlockingStub stub;

  public UserGrpcClient(UserRpcServiceGrpc.UserRpcServiceBlockingStub stub) {
    this.stub = stub;
  }

  public String getUser() {
    GetUserResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getUser(GetUserRequest.newBuilder().build());
    return response.getMessage();
  }
}
