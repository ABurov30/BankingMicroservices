package accountservice.grpc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import account.contract.v1.*;
import accountservice.mapper.command.AccountCommandMapper;
import accountservice.mapper.command.TransferCommandMapper;
import accountservice.mapper.grpc.AccountGrpcMapper;
import accountservice.service.AccountService;
import accountservice.service.TransferService;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccountGrpcServiceTest {
  private final AccountCommandMapper commands = mock(AccountCommandMapper.class);
  private final AccountGrpcMapper responses = mock(AccountGrpcMapper.class);
  private final AccountService accounts = mock(AccountService.class);
  private final TransferService transfers = mock(TransferService.class);
  private final TransferCommandMapper transferCommands = mock(TransferCommandMapper.class);
  private final AccountGrpcService grpc =
      new AccountGrpcService(commands, responses, accounts, transfers, transferCommands);

  @Test
  void handlesAccountGrpcOperations() {
    var health = mock(StreamObserver.class);
    grpc.getAccountHealth(Empty.getDefaultInstance(), health);
    grpc.createAccount(CreateAccountGrpcRequest.getDefaultInstance(), mock(StreamObserver.class));
    when(accounts.getAccountsByOwnerUserId(any())).thenReturn(List.of());
    when(accounts.getAccountsByAuthUserId(any())).thenReturn(List.of());
    when(accounts.getAllAccounts(any())).thenReturn(List.of());
    grpc.getAccountsByOwnerUserId(
        GetAccountByOwnerUserIdGrpcRequest.getDefaultInstance(), mock(StreamObserver.class));
    grpc.getAccountsByAuthUserId(
        GetAccountByAuthUserIdGrpcRequest.getDefaultInstance(), mock(StreamObserver.class));
    grpc.getAllAccounts(GetAllAccountsGrpRequest.getDefaultInstance(), mock(StreamObserver.class));
    grpc.freezeAccount(FreezeAccountGrpcRequest.getDefaultInstance(), mock(StreamObserver.class));
    grpc.unfreezeAccount(
        UnfreezeAccountGrpcRequest.getDefaultInstance(), mock(StreamObserver.class));
    grpc.getAccountById(GetAccountByIdGrpcRequest.getDefaultInstance(), mock(StreamObserver.class));
    when(accounts.getAccountByIdsForTransaction(any())).thenReturn(List.of());
    grpc.getAccountByIdsForTransaction(
        GetAccountByIdsForTransactionGrpcRequest.getDefaultInstance(), mock(StreamObserver.class));
    grpc.topUpAccount(
        UpdateAccountBalanceGrpcRequest.getDefaultInstance(), mock(StreamObserver.class));
    grpc.withdrawAccount(
        UpdateAccountBalanceGrpcRequest.getDefaultInstance(), mock(StreamObserver.class));
    grpc.reserveFundsForTransaction(
        ReserveFundsForTransactionGrpcRequest.getDefaultInstance(), mock(StreamObserver.class));
    when(accounts.getRecipientAccountsByOwnerUserId(any())).thenReturn(List.of());
    grpc.getRecipientAccountsByOwnerUserId(
        GetRecipientAccountsByOwnerUserIdGrpcRequest.getDefaultInstance(),
        mock(StreamObserver.class));
    verify(health).onCompleted();
  }
}
