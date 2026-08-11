package apigateway.client;

import apigateway.dto.transaction.CreateTransactionRequestDto;
import apigateway.mapper.grpc.TransactionGrpcMapper;
import com.google.protobuf.Empty;

import java.util.UUID;
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

    public void createTransaction(CreateTransactionRequestDto request, UUID authUserId) {
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).createTransaction(grpcMapper.toCreateTransactionGrpcRequest(request, authUserId));
    }
}
