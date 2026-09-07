package apigateway.controller;

import apigateway.client.CardGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.dto.request.card.CreateCardRequestDto;
import apigateway.dto.request.card.UpdateCardRequestDto;
import apigateway.dto.response.card.CreateCardResponseDto;
import apigateway.dto.response.card.UpdateCardResponseDto;
import apigateway.dto.result.auth.AuthUserIdAndRoleResult;
import apigateway.query.CardQueryHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
    AuthUserIdAndRoleResult authUser = cookieConfig.getAuthUserIdAndRole(httpRequest);
    return cardQueryHandler.createCard(request, authUser.authUserId(), authUser.role().name());
  }

  @PutMapping("/update")
  public UpdateCardResponseDto updateCard(
      @Valid @RequestBody UpdateCardRequestDto request, HttpServletRequest httpRequest) {
    AuthUserIdAndRoleResult authUser = cookieConfig.getAuthUserIdAndRole(httpRequest);
    return cardQueryHandler.updateCard(request, authUser.authUserId(), authUser.role().name());
  }
}
