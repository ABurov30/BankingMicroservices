package apigateway.controller;

import apigateway.client.CardGrpcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/card")
public class CardGatewayController {

  private final CardGrpcClient cardClient;

  public CardGatewayController(CardGrpcClient cardClient) {
    this.cardClient = cardClient;
  }

  @GetMapping
  public String getCard() {
    return cardClient.getCard();
  }
}
