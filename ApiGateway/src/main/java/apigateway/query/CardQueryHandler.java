package apigateway.query;

import apigateway.client.AccountGrpcClient;
import apigateway.client.CardGrpcClient;
import apigateway.dto.account.GetAccountByIdRequestDto;
import apigateway.dto.account.GetAccountResponseDto;
import apigateway.dto.card.CreateCardRequestDto;
import apigateway.dto.card.CreateCardResponseDto;
import apigateway.dto.card.UpdateCardRequestDto;
import apigateway.dto.card.UpdateCardResponseDto;
import apigateway.exception.AccountNotActiveException;
import enums.account.AccountStatus;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CardQueryHandler {
  private final AccountGrpcClient accountGrpcClient;
  private final CardGrpcClient cardGrpcClient;

  public CardQueryHandler(AccountGrpcClient accountGrpcClient, CardGrpcClient cardGrpcClient) {
    this.accountGrpcClient = accountGrpcClient;
    this.cardGrpcClient = cardGrpcClient;
  }

  public CreateCardResponseDto createCard(
      CreateCardRequestDto request, UUID authUserId, String role) {
    GetAccountResponseDto account = checkAccountStatus(request.accountId());
    return cardGrpcClient.createCard(request, authUserId, role, account);
  }

  public UpdateCardResponseDto updateCard(
      UpdateCardRequestDto request, UUID authUserId, String role) {
    checkAccountStatus(request.accountId());
    return cardGrpcClient.updateCard(request, authUserId, role);
  }

  private GetAccountResponseDto checkAccountStatus(UUID accountId) {
    GetAccountResponseDto account =
        accountGrpcClient.getAccountById(new GetAccountByIdRequestDto(accountId));
    if (account.status() != AccountStatus.ACTIVE) {
      throw new AccountNotActiveException(accountId);
    }
    return account;
  }
}
