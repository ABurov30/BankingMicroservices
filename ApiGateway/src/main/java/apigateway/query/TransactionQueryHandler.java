package apigateway.query;

import apigateway.client.AccountGrpcClient;
import apigateway.client.TransactionGrpcClient;
import apigateway.dto.transaction.CreateTransactionRequestDto;
import apigateway.dto.transaction.CreateTransactionResponseDto;
import apigateway.dto.transaction.TransactionResponseDto;
import apigateway.mapper.grpc.AccountGrpcMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransactionQueryHandler {
  private final AccountGrpcClient accountGrpcClient;
  private final TransactionGrpcClient transactionGrpcClient;
  private final AccountGrpcMapper accountGrpcMapper;

  public TransactionQueryHandler(
      AccountGrpcClient accountGrpcClient,
      TransactionGrpcClient transactionGrpcClient,
      AccountGrpcMapper accountGrpcMapper) {
    this.accountGrpcClient = accountGrpcClient;
    this.transactionGrpcClient = transactionGrpcClient;
    this.accountGrpcMapper = accountGrpcMapper;
  }

  public CreateTransactionResponseDto startTransaction(
      CreateTransactionRequestDto request, UUID authUserId) {
    UUID targetAuthUserId = accountGrpcClient.getAccountOwnerAuthUserId(request.targetAccountId());
    return transactionGrpcClient.createTransaction(request, authUserId, targetAuthUserId);
  }

  public List<TransactionResponseDto> getTransactionsByUserId(UUID userId) {
    var accounts =
        accountGrpcClient.getAccountsByOwnerId(userId).stream()
            .map(accountGrpcMapper::toAccountResponse)
            .toList();

    return transactionGrpcClient.getTransactionsByAccounts(accounts);
  }
}
