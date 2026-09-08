package apigateway.query;

import apigateway.client.AccountGrpcClient;
import apigateway.client.CardGrpcClient;
import apigateway.dto.command.account.CheckAccountStatusCommandDto;
import apigateway.dto.request.card.CreateCardRequestDto;
import apigateway.dto.request.card.UpdateCardRequestDto;
import apigateway.dto.response.account.GetAccountResponseDto;
import apigateway.dto.response.card.CreateCardResponseDto;
import apigateway.dto.response.card.UpdateCardResponseDto;
import apigateway.exception.AccountNotActiveException;
import apigateway.mapper.command.AccountCommandMapper;
import apigateway.mapper.request.AccountRequestMapper;
import enums.account.AccountStatus;
import enums.auth.Roles;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardQueryHandler {
  private final AccountGrpcClient accountGrpcClient;
  private final CardGrpcClient cardGrpcClient;
  private final AccountCommandMapper accountCommandMapper;
  private final AccountRequestMapper accountRequestMapper;

  public CreateCardResponseDto createCard(
      CreateCardRequestDto request, UUID authUserId, String role) {
    GetAccountResponseDto account =
        checkAccountStatus(
            accountCommandMapper.toCheckAccountStatusCommandDto(
                request.accountId(), authUserId, Roles.valueOf(role)));
    return cardGrpcClient.createCard(request, authUserId, role, account);
  }

  public UpdateCardResponseDto updateCard(
      UpdateCardRequestDto request, UUID authUserId, String role) {
    checkAccountStatus(
        accountCommandMapper.toCheckAccountStatusCommandDto(
            request.accountId(), authUserId, Roles.valueOf(role)));
    return cardGrpcClient.updateCard(request, authUserId, role);
  }

  private GetAccountResponseDto checkAccountStatus(CheckAccountStatusCommandDto command) {
    GetAccountResponseDto account =
        accountGrpcClient.getAccountById(accountRequestMapper.toGetAccountByIdRequestDto(command));
    if (account.status() != AccountStatus.ACTIVE) {
      throw new AccountNotActiveException(command.accountId());
    }
    return account;
  }
}
