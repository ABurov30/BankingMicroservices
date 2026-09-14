package transactionservice.client;

import account.contract.v1.*;
import io.grpc.Status;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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

  public Map<UUID, AccountResponse> getAccountByIdsForTransaction(List<UUID> accountIds) {
    var ids = accountIds.stream().distinct().toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    var request =
        GetAccountByIdsForTransactionGrpcRequest.newBuilder()
            .addAllAccountId(ids.stream().map(UUID::toString).toList())
            .build();
    var response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getAccountByIdsForTransaction(request);
    var accounts =
        response.getAccountList().stream()
            .collect(
                Collectors.toMap(
                    account -> UUID.fromString(account.getAccountId()), value -> value));
    for (var id : ids) {
      if (!accounts.containsKey(id)) {
        throw Status.NOT_FOUND.withDescription("Account not found: " + id).asRuntimeException();
      }
    }
    return accounts;
  }
}
