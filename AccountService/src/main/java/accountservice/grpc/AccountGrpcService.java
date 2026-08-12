package accountservice.grpc;

import account.contract.v1.*;
import accountservice.dto.CreateAccountCommand;
import accountservice.dto.GetAccountResult;
import accountservice.dto.GetAccountsByOwnerUserIdCommand;
import accountservice.mapper.command.AccountCommandMapper;
import accountservice.mapper.command.TransferCommandMapper;
import accountservice.mapper.grpc.AccountGrpcMapper;
import accountservice.service.AccountService;
import accountservice.service.TransferService;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AccountGrpcService extends AccountRpcServiceGrpc.AccountRpcServiceImplBase {

  private final AccountCommandMapper commandMapper;
  private final AccountGrpcMapper grpcMapper;
  private final AccountService accountService;
  private final TransferService transferService;
  private final TransferCommandMapper transferCommandMapper;

  public AccountGrpcService(
      AccountCommandMapper commandMapper,
      AccountGrpcMapper grpcMapper,
      AccountService accountService,
      TransferService transferService,
      TransferCommandMapper transferCommandMapper) {
    this.commandMapper = commandMapper;
    this.grpcMapper = grpcMapper;
    this.accountService = accountService;
    this.transferService = transferService;
    this.transferCommandMapper = transferCommandMapper;
  }

  @Override
  public void getAccountHealth(
      Empty request, StreamObserver<GetAccountHealthGrpcResponse> responseObserver) {
    GetAccountHealthGrpcResponse response =
        GetAccountHealthGrpcResponse.newBuilder()
            .setMessage("Account service GRPC health " + LocalDateTime.now())
            .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void createAccount(
      CreateAccountGrpcRequest request,
      StreamObserver<CreateAccountGrpcResponse> responseObserver) {
    CreateAccountCommand updateAccountCommand = commandMapper.toCreateAccountCommand(request);
    CreateAccountGrpcResponse response =
        grpcMapper.toCreateAccountGrpcResponse(accountService.createAccount(updateAccountCommand));

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getAccountsByOwnerUserId(
      GetAccountByOwnerUserIdGrpcRequest request,
      StreamObserver<GetAccountsGrpcResponse> responseObserver) {
    GetAccountsByOwnerUserIdCommand command =
        commandMapper.toGetAccountsByOwnerUserIdCommand(request);
    List<GetAccountResult> result = accountService.getAccountsByOwnerUserId(command);
    List<AccountResponse> accountResponseList =
        result.stream().map(grpcMapper::toAccountResponse).toList();

    responseObserver.onNext(grpcMapper.toGetAccountsGrpcResponse(accountResponseList));
    responseObserver.onCompleted();
  }

  @Override
  public void getAllAccounts(
      Empty request, StreamObserver<GetAccountsGrpcResponse> responseObserver) {
    List<GetAccountResult> result = accountService.getAllAccounts();
    List<AccountResponse> accountResponseList =
        result.stream().map(grpcMapper::toAccountResponse).toList();

    responseObserver.onNext(grpcMapper.toGetAccountsGrpcResponse(accountResponseList));
    responseObserver.onCompleted();
  }

  @Override
  public void freezeAccount(
      FreezeAccountGrpcRequest request, StreamObserver<Empty> responseObserver) {
    accountService.freezeAccount(commandMapper.toFreezeAccountCommand(request));

    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void unfreezeAccount(
      UnfreezeAccountGrpcRequest request, StreamObserver<Empty> responseObserver) {
    accountService.unfreezeAccount(commandMapper.toUnfreezeAccountCommand(request));

    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void getAccountById(
      GetAccountByIdGrpcRequest request,
      StreamObserver<GetAccountByIdGrpcResponse> responseObserver) {
    GetAccountResult result =
        accountService.getAccountById(commandMapper.toGetAccountByIdCommand(request));
    AccountResponse response = grpcMapper.toAccountResponse(result);
    responseObserver.onNext(grpcMapper.toGetAccountByIdGrpcResponse(response));
    responseObserver.onCompleted();
  }

  @Override
  public void topUpAccount(
      UpdateAccountBalanceGrpcRequest request, StreamObserver<AccountResponse> responseObserver) {
    GetAccountResult result =
        accountService.topUpAccount(commandMapper.toUpdateAccountBalanceCommand(request));
    responseObserver.onNext(grpcMapper.toAccountResponse(result));
    responseObserver.onCompleted();
  }

  @Override
  public void withdrawAccount(
      UpdateAccountBalanceGrpcRequest request, StreamObserver<AccountResponse> responseObserver) {
    GetAccountResult result =
        accountService.withdrawAccount(commandMapper.toUpdateAccountBalanceCommand(request));
    responseObserver.onNext(grpcMapper.toAccountResponse(result));
    responseObserver.onCompleted();
  }

  @Override
  public void reserveFundsForTransaction(
      ReserveFundsForTransactionGrpcRequest request,
      StreamObserver<ReserveFundsForTransactionGrpcResponse> responseObserver) {
    var result =
        transferService.reserveFundsForTransactional(
            transferCommandMapper.toReserveFundsForTransactionCommand(request));

    responseObserver.onNext(grpcMapper.toReserveFundsForTransactionGrpcResponse(result));
    responseObserver.onCompleted();
  }
}
