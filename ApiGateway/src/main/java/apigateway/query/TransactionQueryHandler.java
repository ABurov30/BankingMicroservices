package apigateway.query;

import apigateway.client.AccountGrpcClient;
import apigateway.client.TransactionGrpcClient;
import apigateway.dto.transaction.CreateTransactionRequestDto;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TransactionQueryHandler {
    private final AccountGrpcClient accountGrpcClient;
    private final TransactionGrpcClient transactionGrpcClient;

    public TransactionQueryHandler (
            AccountGrpcClient accountGrpcClient,
            TransactionGrpcClient transactionGrpcClient
    ) {
        this.accountGrpcClient = accountGrpcClient;
        this.transactionGrpcClient = transactionGrpcClient;
    }

    public void startTransaction(CreateTransactionRequestDto request, UUID authUserId) {
        UUID targetAuthUserId = accountGrpcClient.getAccountOwnerAuthUserId(request.targetAccountId());
        transactionGrpcClient.createTransaction(request, authUserId, targetAuthUserId);
    }
}
