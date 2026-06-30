package apigateway.controller;

import apigateway.client.AccountGrpcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
public class AccountGatewayController {

  private final AccountGrpcClient accountClient;

  public AccountGatewayController(AccountGrpcClient accountClient) {
    this.accountClient = accountClient;
  }

  @GetMapping
  public String getAccount() {
    return accountClient.getAccount();
  }
}
