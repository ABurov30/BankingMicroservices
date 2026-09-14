package apigateway.query;

import apigateway.client.AccountGrpcClient;
import apigateway.client.CardGrpcClient;
import apigateway.client.UserGrpcClient;
import apigateway.dto.command.account.CreateAccountCommandDto;
import apigateway.dto.command.account.GetAccountsWithCardsByOwnerIdCommandDto;
import apigateway.dto.command.account.GetAllAccountsWithCardsByAuthUserIdCommandDto;
import apigateway.dto.command.account.GetAllAccountsWithCardsCommandDto;
import apigateway.dto.command.card.GetCardsByAccountIdsCommandDto;
import apigateway.dto.response.account.CreateAccountResponseDto;
import apigateway.dto.response.account.GetAccountResponseDto;
import apigateway.dto.response.account.GetAccountWithCardsResponseDto;
import apigateway.dto.response.card.GetCardByAccountIdResponseDto;
import apigateway.mapper.request.AccountRequestMapper;
import apigateway.mapper.request.UserRequestMapper;
import enums.auth.Roles;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountQueryHandler {
  private final AccountGrpcClient accountGrpcClient;
  private final CardGrpcClient cardGrpcClient;
  private final AccountRequestMapper accountRequestMapper;
  private final UserGrpcClient userGrpcClient;
  private final UserRequestMapper userRequestMapper;

  public List<GetAccountWithCardsResponseDto> getAllAccountsWithCards(
      GetAllAccountsWithCardsCommandDto command) {
    List<GetAccountResponseDto> accounts =
        accountGrpcClient.getAllAccounts(accountRequestMapper.toGetAllAccountsRequestDto(command));

    return withCards(accounts, command.authUserId(), command.role());
  }

  public List<GetAccountWithCardsResponseDto> getAccountsWithCardsByOwnerId(
      GetAccountsWithCardsByOwnerIdCommandDto command) {
    List<GetAccountResponseDto> accounts =
        accountGrpcClient.getAccountsByOwnerId(
            accountRequestMapper.toGetAccountsWithCardsByOwnerIdRequestDto(command));

    return withCards(accounts, command.authUserId(), command.role());
  }

  public List<GetAccountWithCardsResponseDto> getAccountsWithCardsByAuthUserId(
      GetAllAccountsWithCardsByAuthUserIdCommandDto command) {

    List<GetAccountResponseDto> accounts =
        accountGrpcClient.getAccountsByAuthUserId(
            accountRequestMapper.toGetAccountsByAuthUserIdRequestDto(command));

    return withCards(accounts, command.authUserId(), command.role());
  }

  private List<GetAccountWithCardsResponseDto> withCards(
      List<GetAccountResponseDto> accounts, UUID authUserId, Roles role) {
    if (accounts.isEmpty()) {
      return List.of();
    }
    var accountIds = accounts.stream().map(GetAccountResponseDto::accountId).distinct().toList();
    var cardsByAccount =
        cardGrpcClient
            .getCardsByAccountIds(new GetCardsByAccountIdsCommandDto(accountIds, authUserId, role))
            .stream()
            .collect(Collectors.groupingBy(GetCardByAccountIdResponseDto::accountId));
    return accounts.stream()
        .map(
            account ->
                new GetAccountWithCardsResponseDto(
                    account, cardsByAccount.getOrDefault(account.accountId(), List.of())))
        .toList();
  }

  public CreateAccountResponseDto createAccount(CreateAccountCommandDto commandDto) {
    var userInfo =
        userGrpcClient.getUserInfo(
            userRequestMapper.toGetUserInfoRequestDto(commandDto.authUserId()));
    return accountGrpcClient.createAccount(
        accountRequestMapper.toCreateAccountRequestDto(commandDto),
        commandDto.authUserId(),
        userInfo.userProfileId());
  }
}
