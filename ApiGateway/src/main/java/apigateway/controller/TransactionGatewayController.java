package apigateway.controller;

import apigateway.client.TransactionGrpcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transaction")
public class TransactionGatewayController {

  private final TransactionGrpcClient transactionClient;

  public TransactionGatewayController(TransactionGrpcClient transactionClient) {
    this.transactionClient = transactionClient;
  }

  @GetMapping
  public String getTransaction() {
    return transactionClient.getTransaction();
  }
}
