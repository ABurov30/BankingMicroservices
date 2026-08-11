package transactionservice.client;

import account.contract.v1.*;
import org.springframework.stereotype.Service;
import transactionservice.dto.ReserveFudsForTransactionResponseDto;
import transactionservice.mapper.dto.TransactionDtoMapper;

import java.util.concurrent.TimeUnit;


@Service
public class AccountGrpcClient {
    private final AccountRpcServiceGrpc.AccountRpcServiceBlockingStub stub;
    private final TransactionDtoMapper dtoMapper;

    public AccountGrpcClient(
            AccountRpcServiceGrpc.AccountRpcServiceBlockingStub stub,
            TransactionDtoMapper transactionDtoMapper
    ) {
        this.stub = stub;
        this.dtoMapper = transactionDtoMapper;
    }

    public ReserveFudsForTransactionResponseDto reserveFundsForTransaction(ReserveFundsForTransactionGrpcRequest grpcRequest) {
        return dtoMapper.toReserveFudsForTransactionResponseDto(stub.withDeadlineAfter(2, TimeUnit.SECONDS).reserveFundsForTransaction(grpcRequest));
    }
}
