package apigateway.controller;

import apigateway.client.TransactionGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.dto.request.transaction.CreateTransactionRequestDto;
import apigateway.dto.response.transaction.CreateTransactionResponseDto;
import apigateway.dto.response.transaction.TransactionResponseDto;
import apigateway.dto.result.auth.AuthUserIdAndRoleResult;
import apigateway.mapper.command.TransactionCommandMapper;
import apigateway.query.TransactionQueryHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transaction")
public class TransactionGatewayController {

  private final TransactionGrpcClient transactionClient;
  private final TransactionQueryHandler transactionQueryHandler;
  private final CookieConfig cookieConfig;
  private final TransactionCommandMapper transactionCommandMapper;

  public TransactionGatewayController(
      TransactionGrpcClient transactionClient,
      TransactionQueryHandler transactionQueryHandler,
      CookieConfig cookieConfig,
      TransactionCommandMapper transactionCommandMapper) {
    this.transactionClient = transactionClient;
    this.transactionQueryHandler = transactionQueryHandler;
    this.cookieConfig = cookieConfig;
    this.transactionCommandMapper = transactionCommandMapper;
  }

  @GetMapping("/health")
  public String getTransactionHealth() {
    return transactionClient.getTransactionHealth();
  }

  @PostMapping("/creat-transaction")
  public CreateTransactionResponseDto createTransaction(
      @Valid @RequestBody CreateTransactionRequestDto request, HttpServletRequest httpRequest) {
    return transactionQueryHandler.startTransaction(
        request, cookieConfig.getAuthUserId(httpRequest));
  }

  @GetMapping("/user/me")
  public List<TransactionResponseDto> getTransactionsByMe(HttpServletRequest httpRequest) {
    AuthUserIdAndRoleResult authUser = cookieConfig.getAuthUserIdAndRole(httpRequest);
    return transactionQueryHandler.getTransactionsByMe(
        transactionCommandMapper.toGetTransactionByMeCommandDto(
            authUser.authUserId(), authUser.role()));
  }

  @GetMapping("/manager/user/{userId}")
  public List<TransactionResponseDto> getTransactionsByUserIdByManager(
      @PathVariable UUID userId, HttpServletRequest httpRequest) {
    AuthUserIdAndRoleResult authUser = cookieConfig.getAuthUserIdAndRole(httpRequest);
    return transactionQueryHandler.getTransactionsByUserId(
        transactionCommandMapper.toGetTransactionByUserIdCommandDto(
            userId, authUser.authUserId(), authUser.role()));
  }
}
