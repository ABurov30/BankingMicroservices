package apigateway.controller;

import apigateway.client.TransactionGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.dto.transaction.CreateTransactionRequestDto;
import apigateway.dto.transaction.TransactionResponseDto;
import apigateway.query.TransactionQueryHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transaction")
public class TransactionGatewayController {

  private final TransactionGrpcClient transactionClient;
  private final TransactionQueryHandler transactionQueryHandler;
  private final CookieConfig cookieConfig;

  public TransactionGatewayController(
      TransactionGrpcClient transactionClient,
      TransactionQueryHandler transactionQueryHandler,
      CookieConfig cookieConfig) {
    this.transactionClient = transactionClient;
    this.transactionQueryHandler = transactionQueryHandler;
    this.cookieConfig = cookieConfig;
  }

  @GetMapping("/health")
  public String getTransactionHealth() {
    return transactionClient.getTransactionHealth();
  }

  @PostMapping("/creat-transaction")
  public void createTransaction(
      @Valid @RequestBody CreateTransactionRequestDto request, HttpServletRequest httpRequest) {
    Jwt jwt = cookieConfig.getAccessTokenJwt(httpRequest);
    UUID authUserId = UUID.fromString(jwt.getSubject());

    transactionQueryHandler.startTransaction(request, authUserId);
  }

  @GetMapping("/user/{userId}")
  public List<TransactionResponseDto> getTransactionsByUserId(@PathVariable UUID userId) {
    return transactionQueryHandler.getTransactionsByUserId(userId);
  }
}
