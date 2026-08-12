package apigateway.controller;

import apigateway.client.CardGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.dto.card.CreateCardRequestDto;
import apigateway.dto.card.CreateCardResponseDto;
import apigateway.dto.card.UpdateCardRequestDto;
import apigateway.dto.card.UpdateCardResponseDto;
import apigateway.query.CardQueryHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/card")
public class CardGatewayController {

  private final CardGrpcClient cardClient;
  private final CookieConfig cookieConfig;
  private final CardQueryHandler cardQueryHandler;

  public CardGatewayController(
      CardGrpcClient cardClient, CookieConfig cookieConfig, CardQueryHandler cardQueryHandler) {
    this.cardClient = cardClient;
    this.cookieConfig = cookieConfig;
    this.cardQueryHandler = cardQueryHandler;
  }

  @GetMapping("/health")
  public String getCardHealth() {
    return cardClient.getCardHealth();
  }

  @PostMapping("/create")
  public CreateCardResponseDto createCard(
      @Valid @RequestBody CreateCardRequestDto request, HttpServletRequest httpRequest) {
    Jwt jwt = cookieConfig.getAccessTokenJwt(httpRequest);
    return cardQueryHandler.createCard(
        request, UUID.fromString(jwt.getSubject()), cookieConfig.extractRole(jwt));
  }

  @PutMapping("/update")
  public UpdateCardResponseDto updateCard(
      @Valid @RequestBody UpdateCardRequestDto request, HttpServletRequest httpRequest) {
    Jwt jwt = cookieConfig.getAccessTokenJwt(httpRequest);
    return cardQueryHandler.updateCard(
        request, UUID.fromString(jwt.getSubject()), cookieConfig.extractRole(jwt));
  }
}
