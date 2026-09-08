package apigateway.query;

import apigateway.client.AccountGrpcClient;
import apigateway.client.CardGrpcClient;
import apigateway.client.UserGrpcClient;
import apigateway.dto.command.account.CreateAccountCommandDto;
import apigateway.dto.command.account.GetAccountsWithCardsByOwnerIdCommandDto;
import apigateway.dto.command.account.GetAllAccountsWithCardsByAuthUserIdCommandDto;
import apigateway.dto.command.account.GetAllAccountsWithCardsCommandDto;
import apigateway.dto.response.account.CreateAccountResponseDto;
import apigateway.dto.response.account.GetAccountResponseDto;
import apigateway.dto.response.account.GetAccountWithCardsResponseDto;
import apigateway.mapper.command.CardCommandMapper;
import apigateway.mapper.request.AccountRequestMapper;
import apigateway.mapper.request.UserRequestMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountQueryHandler {
  private final AccountGrpcClient accountGrpcClient;
  private final CardGrpcClient cardGrpcClient;
  private final CardCommandMapper cardCommandMapper;
  private final AccountRequestMapper accountRequestMapper;
  private final UserGrpcClient userGrpcClient;
  private final UserRequestMapper userRequestMapper;

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
    List<GetAccountResponseDto> accounts =
        accountGrpcClient.getAccountsByOwnerId(
            accountRequestMapper.toGetAccountsWithCardsByOwnerIdRequestDto(command));

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

  public List<GetAccountWithCardsResponseDto> getAccountsWithCardsByAuthUserId(
      GetAllAccountsWithCardsByAuthUserIdCommandDto command) {
    List<GetAccountResponseDto> accounts =
        accountGrpcClient.getAccountsByAuthUserId(
            accountRequestMapper.toGetAccountsByAuthUserIdRequestDto(command));

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
