package apigateway.controller;

import apigateway.client.UserGrpcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserGatewayController {

  private final UserGrpcClient userClient;

  public UserGatewayController(UserGrpcClient userClient) {
    this.userClient = userClient;
  }

  @GetMapping
  public String getUser() {
    return userClient.getUser();
  }
}
