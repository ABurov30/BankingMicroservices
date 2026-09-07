package apigateway.query;

import apigateway.client.AccountGrpcClient;
import apigateway.client.CardGrpcClient;
import apigateway.dto.command.account.GetAccountsWithCardsByOwnerIdCommandDto;
import apigateway.dto.command.account.GetAllAccountsWithCardsCommandDto;
import apigateway.dto.response.account.GetAccountResponseDto;
import apigateway.dto.response.account.GetAccountWithCardsResponseDto;
import apigateway.mapper.command.CardCommandMapper;
import apigateway.mapper.request.AccountRequestMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AccountQueryHandler {
  private final AccountGrpcClient accountGrpcClient;
  private final CardGrpcClient cardGrpcClient;
  private final CardCommandMapper cardCommandMapper;
  private final AccountRequestMapper accountRequestMapper;

  public AccountQueryHandler(
      AccountGrpcClient accountGrpcClient,
      CardGrpcClient cardGrpcClient,
      CardCommandMapper cardCommandMapper,
      AccountRequestMapper accountRequestMapper) {
    this.accountGrpcClient = accountGrpcClient;
    this.cardGrpcClient = cardGrpcClient;
    this.cardCommandMapper = cardCommandMapper;
    this.accountRequestMapper = accountRequestMapper;
  }

  public List<GetAccountWithCardsResponseDto> getAllAccountsWithCards(
      GetAllAccountsWithCardsCommandDto command) {
    List<GetAccountResponseDto> accounts =
        accountGrpcClient.getAllAccounts(accountRequestMapper.toGetAllAccountsRequestDto(command));

    return accounts.stream()
        .map(
            account ->
                new GetAccountWithCardsResponseDto(
                    account,
                    cardGrpcClient.getCardsByAccountId(
                        cardCommandMapper.toGetCardsByAccountIdCommandDto(
                            account.accountId(), command.authUserId(), command.role()))))
        .toList();
  }

  public List<GetAccountWithCardsResponseDto> getAccountsWithCardsByOwnerId(
      GetAccountsWithCardsByOwnerIdCommandDto command) {
    List<GetAccountResponseDto> accounts = accountGrpcClient.getAccountsByOwnerId(command);

    return accounts.stream()
        .map(
            account ->
                new GetAccountWithCardsResponseDto(
                    account,
                    cardGrpcClient.getCardsByAccountId(
                        cardCommandMapper.toGetCardsByAccountIdCommandDto(
                            account.accountId(), command.authUserId(), command.role()))))
        .toList();
  }
}
