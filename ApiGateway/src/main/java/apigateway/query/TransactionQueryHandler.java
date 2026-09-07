package apigateway.query;

import apigateway.client.AccountGrpcClient;
import apigateway.client.TransactionGrpcClient;
import apigateway.dto.command.transaction.GetTransactionByUserIdCommandDto;
import apigateway.dto.request.transaction.CreateTransactionRequestDto;
import apigateway.dto.response.transaction.CreateTransactionResponseDto;
import apigateway.dto.response.transaction.TransactionResponseDto;
import apigateway.mapper.command.AccountCommandMapper;
import apigateway.mapper.grpc.AccountGrpcMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransactionQueryHandler {
  private final AccountGrpcClient accountGrpcClient;
  private final TransactionGrpcClient transactionGrpcClient;
  private final AccountGrpcMapper accountGrpcMapper;
  private final AccountCommandMapper accountCommandMapper;

  public TransactionQueryHandler(
      AccountGrpcClient accountGrpcClient,
      TransactionGrpcClient transactionGrpcClient,
      AccountGrpcMapper accountGrpcMapper,
      AccountCommandMapper accountCommandMapper) {
    this.accountGrpcClient = accountGrpcClient;
    this.transactionGrpcClient = transactionGrpcClient;
    this.accountGrpcMapper = accountGrpcMapper;
    this.accountCommandMapper = accountCommandMapper;
  }

  public CreateTransactionResponseDto startTransaction(
      CreateTransactionRequestDto request, UUID authUserId) {
    return transactionGrpcClient.createTransaction(request, authUserId);
  }

  public List<TransactionResponseDto> getTransactionsByUserId(
      GetTransactionByUserIdCommandDto command) {
    var accounts =
        accountGrpcClient
            .getAccountsByOwnerId(
                accountCommandMapper.toGetAccountsWithCardsByOwnerIdCommandDto(command))
            .stream()
            .map(accountGrpcMapper::toAccountResponse)
            .toList();

    return transactionGrpcClient.getTransactionsByAccounts(accounts);
  }
}
