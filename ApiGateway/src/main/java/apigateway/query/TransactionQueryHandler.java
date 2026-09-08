package apigateway.query;

import apigateway.client.AccountGrpcClient;
import apigateway.client.TransactionGrpcClient;
import apigateway.client.UserGrpcClient;
import apigateway.dto.command.transaction.GetTransactionByMeCommandDto;
import apigateway.dto.command.transaction.GetTransactionByUserIdCommandDto;
import apigateway.dto.request.transaction.CreateTransactionRequestDto;
import apigateway.dto.response.transaction.CreateTransactionResponseDto;
import apigateway.dto.response.transaction.TransactionResponseDto;
import apigateway.mapper.command.TransactionCommandMapper;
import apigateway.mapper.grpc.AccountGrpcMapper;
import apigateway.mapper.request.AccountRequestMapper;
import apigateway.mapper.request.UserRequestMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransactionQueryHandler {
  private final AccountGrpcClient accountGrpcClient;
  private final TransactionGrpcClient transactionGrpcClient;
  private final AccountGrpcMapper accountGrpcMapper;
  private final AccountRequestMapper accountRequestMapper;
  private final UserGrpcClient userGrpcClient;
  private final UserRequestMapper userRequestMapper;
  private final TransactionCommandMapper transactionCommandMapper;

  public TransactionQueryHandler(
      AccountGrpcClient accountGrpcClient,
      TransactionGrpcClient transactionGrpcClient,
      AccountGrpcMapper accountGrpcMapper,
      AccountRequestMapper accountRequestMapper,
      UserGrpcClient userGrpcClient,
      UserRequestMapper userRequestMapper,
      TransactionCommandMapper transactionCommandMapper) {
    this.accountGrpcClient = accountGrpcClient;
    this.transactionGrpcClient = transactionGrpcClient;
    this.accountGrpcMapper = accountGrpcMapper;
    this.accountRequestMapper = accountRequestMapper;
    this.userGrpcClient = userGrpcClient;
    this.userRequestMapper = userRequestMapper;
    this.transactionCommandMapper = transactionCommandMapper;
  }

  public CreateTransactionResponseDto startTransaction(
      CreateTransactionRequestDto request, UUID authUserId) {
    return transactionGrpcClient.createTransaction(request, authUserId);
  }

  public List<TransactionResponseDto> getTransactionsByMe(GetTransactionByMeCommandDto commandDto) {
    var userInfo =
        userGrpcClient.getUserInfo(
            userRequestMapper.toGetUserInfoRequestDto(commandDto.authUserId()));
    return getTransactionsByUserId(
        transactionCommandMapper.toGetTransactionByUserIdCommandDto(
            userInfo.userProfileId(), commandDto.authUserId(), commandDto.role()));
  }

  public List<TransactionResponseDto> getTransactionsByUserId(
      GetTransactionByUserIdCommandDto command) {
    var accounts =
        accountGrpcClient
            .getAccountsByOwnerId(
                accountRequestMapper.toGetAccountsWithCardsByOwnerIdRequestDto(command))
            .stream()
            .map(accountGrpcMapper::toAccountResponse)
            .toList();

    return transactionGrpcClient.getTransactionsByAccounts(accounts);
  }
}
