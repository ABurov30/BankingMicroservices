package transactionservice.client;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import account.contract.v1.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import transactionservice.mapper.dto.TransactionDtoMapper;

class AccountGrpcClientTest {
  private final AccountRpcServiceGrpc.AccountRpcServiceBlockingStub stub =
      mock(AccountRpcServiceGrpc.AccountRpcServiceBlockingStub.class, RETURNS_SELF);
  private final AccountGrpcClient client =
      new AccountGrpcClient(stub, mock(TransactionDtoMapper.class));

  @Test
  void sendsOneDeduplicatedBatchAndMapsUnorderedResponse() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    var request =
        GetAccountByIdsForTransactionGrpcRequest.newBuilder()
            .addAccountId(first.toString())
            .addAccountId(second.toString())
            .build();
    var firstAccount = AccountResponse.newBuilder().setAccountId(first.toString()).build();
    var secondAccount = AccountResponse.newBuilder().setAccountId(second.toString()).build();
    when(stub.getAccountByIdsForTransaction(request))
        .thenReturn(
            GetAccountByIdGrpcResponses.newBuilder()
                .addAccount(secondAccount)
                .addAccount(firstAccount)
                .build());
    var result = client.getAccountByIdsForTransaction(List.of(first, second, first));
    assertThat(result).containsEntry(first, firstAccount).containsEntry(second, secondAccount);
    verify(stub).withDeadlineAfter(2, TimeUnit.SECONDS);
    verify(stub).getAccountByIdsForTransaction(request);
    verifyNoMoreInteractions(stub);
  }

  @Test
  void emptyInputSkipsRpc() {
    assertThat(client.getAccountByIdsForTransaction(List.of())).isEmpty();
    verifyNoInteractions(stub);
  }

  @Test
  void incompleteResponseFailsWithNotFound() {
    var id = UUID.randomUUID();
    var request =
        GetAccountByIdsForTransactionGrpcRequest.newBuilder().addAccountId(id.toString()).build();
    when(stub.getAccountByIdsForTransaction(request))
        .thenReturn(GetAccountByIdGrpcResponses.getDefaultInstance());
    assertThatThrownBy(() -> client.getAccountByIdsForTransaction(List.of(id)))
        .isInstanceOfSatisfying(
            StatusRuntimeException.class,
            error -> assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND));
  }
}
