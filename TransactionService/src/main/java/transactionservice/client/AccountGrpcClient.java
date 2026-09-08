package transactionservice.client;

import account.contract.v1.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import transactionservice.dto.ReserveFudsForTransactionResponseDto;
import transactionservice.mapper.dto.TransactionDtoMapper;

@Service
@RequiredArgsConstructor
public class AccountGrpcClient {
  private final AccountRpcServiceGrpc.AccountRpcServiceBlockingStub stub;
  private final TransactionDtoMapper dtoMapper;

  public ReserveFudsForTransactionResponseDto reserveFundsForTransaction(
      ReserveFundsForTransactionGrpcRequest grpcRequest) {
    return dtoMapper.toReserveFudsForTransactionResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).reserveFundsForTransaction(grpcRequest));
  }

  public AccountResponse getAccountByIdForTransaction(UUID accountId) {
    GetAccountByIdForTransactionGrpcRequest request =
        GetAccountByIdForTransactionGrpcRequest.newBuilder()
            .setAccountId(accountId.toString())
            .build();
    return stub.withDeadlineAfter(2, TimeUnit.SECONDS)
        .getAccountByIdForTransaction(request)
        .getAccount();
  }
}
