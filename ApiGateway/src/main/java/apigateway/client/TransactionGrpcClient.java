package apigateway.client;

import account.contract.v1.AccountResponse;
import apigateway.dto.account.GetAccountResponseDto;
import apigateway.dto.transaction.CreateTransactionRequestDto;
import apigateway.dto.transaction.TransactionResponseDto;
import apigateway.mapper.grpc.TransactionGrpcMapper;
import com.google.protobuf.Empty;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import transaction.contract.v1.*;

@Service
public class TransactionGrpcClient {
    private final TransactionRpcServiceGrpc.TransactionRpcServiceBlockingStub stub;
    private final TransactionGrpcMapper grpcMapper;

    public TransactionGrpcClient(
            TransactionRpcServiceGrpc.TransactionRpcServiceBlockingStub stub,
            TransactionGrpcMapper grpcMapper
    ) {
        this.stub = stub;
        this.grpcMapper = grpcMapper;
    }

    public String getTransactionHealth() {
        GetTransactionHealthGrpcResponse response =
                stub.withDeadlineAfter(2, TimeUnit.SECONDS)
                        .getTransactionHealth(Empty.getDefaultInstance());
        return response.getMessage();
    }

    public void createTransaction(
            CreateTransactionRequestDto request,
            UUID sourceAuthUserId,
            UUID targetAuthUserId
    ) {
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
                .createTransaction(grpcMapper.toCreateTransactionGrpcRequest(request, sourceAuthUserId, targetAuthUserId));
    }

    public List<TransactionResponseDto> getTransactionsByAccounts(List<AccountResponse> accounts) {
        GetTransactionsByAccountsGrpcResponse response = stub.withDeadlineAfter(2, TimeUnit.SECONDS)
                .getTransactionsByAccounts(grpcMapper.toGetTransactionsByAccountsGrpcRequest(accounts));
        return grpcMapper.toTransactionResponseDtos(response);
    }
}
