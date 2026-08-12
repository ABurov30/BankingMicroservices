package transactionservice.client;

import account.contract.v1.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import transactionservice.dto.ReserveFudsForTransactionResponseDto;
import transactionservice.mapper.dto.TransactionDtoMapper;

@Service
public class AccountGrpcClient {
  private final AccountRpcServiceGrpc.AccountRpcServiceBlockingStub stub;
  private final TransactionDtoMapper dtoMapper;

  public AccountGrpcClient(
      AccountRpcServiceGrpc.AccountRpcServiceBlockingStub stub,
      TransactionDtoMapper transactionDtoMapper) {
    this.stub = stub;
    this.dtoMapper = transactionDtoMapper;
  }

  public ReserveFudsForTransactionResponseDto reserveFundsForTransaction(
      ReserveFundsForTransactionGrpcRequest grpcRequest) {
    return dtoMapper.toReserveFudsForTransactionResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).reserveFundsForTransaction(grpcRequest));
  }

  public AccountResponse getAccountById(UUID accountId) {
    GetAccountByIdGrpcRequest request =
        GetAccountByIdGrpcRequest.newBuilder().setAccountId(accountId.toString()).build();
    return stub.withDeadlineAfter(2, TimeUnit.SECONDS).getAccountById(request).getAccount();
  }
}
