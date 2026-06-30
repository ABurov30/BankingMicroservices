package apigateway.controller;

import apigateway.client.AuthGrpcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthGatewayController {

  private final AuthGrpcClient authClient;

  public AuthGatewayController(AuthGrpcClient authClient) {
    this.authClient = authClient;
  }

  @GetMapping
  public String getAuth() {
    return authClient.getAuth();
  }
}
