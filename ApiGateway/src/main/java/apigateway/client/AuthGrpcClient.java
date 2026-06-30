package apigateway.client;

import auth.contract.v1.AuthRpcServiceGrpc;
import auth.contract.v1.GetAuthRequest;
import auth.contract.v1.GetAuthResponse;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class AuthGrpcClient {
  private final AuthRpcServiceGrpc.AuthRpcServiceBlockingStub stub;

  public AuthGrpcClient(AuthRpcServiceGrpc.AuthRpcServiceBlockingStub stub) {
    this.stub = stub;
  }

  public String getAuth() {
    GetAuthResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getAuth(GetAuthRequest.newBuilder().build());
    return response.getMessage();
  }
}
