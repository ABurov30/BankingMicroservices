package transactionservice.grpc;

import account.contract.v1.AccountResponse;
import com.google.protobuf.Empty;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import transaction.contract.v1.*;
import transactionservice.client.AccountGrpcClient;
import transactionservice.dto.CreateTransactionResult;
import transactionservice.mapper.command.TransactionCommandMapper;
import transactionservice.mapper.grpc.TransactionGrpcMapper;
import transactionservice.service.TransactionService;

@Service
public class TransactionGrpcService
    extends TransactionRpcServiceGrpc.TransactionRpcServiceImplBase {

  private final TransactionService transactionService;
  private final AccountGrpcClient accountGrpcClient;
  private final TransactionCommandMapper commandMapper;
  private final TransactionGrpcMapper grpcMapper;
  private final TransactionStatusStreamRegistry transactionStatusStreamRegistry;

  public TransactionGrpcService(
      TransactionService transactionService,
      AccountGrpcClient accountGrpcClient,
      TransactionCommandMapper transactionCommandMapper,
      TransactionGrpcMapper transactionGrpcMapper,
      TransactionStatusStreamRegistry transactionStatusStreamRegistry) {
    this.transactionService = transactionService;
    this.accountGrpcClient = accountGrpcClient;
    this.commandMapper = transactionCommandMapper;
    this.grpcMapper = transactionGrpcMapper;
    this.transactionStatusStreamRegistry = transactionStatusStreamRegistry;
  }

  @Override
  public void getTransactionHealth(
      Empty request, StreamObserver<GetTransactionHealthGrpcResponse> responseObserver) {
    GetTransactionHealthGrpcResponse response =
        GetTransactionHealthGrpcResponse.newBuilder()
            .setMessage("Transaction service GRPC health " + LocalDateTime.now())
            .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void createTransaction(
      CreateTransactionGrpcRequest request,
      StreamObserver<CreateTransactionGrpcResponse> responseObserver) {
    CreateTransactionResult transactionResult =
        transactionService.createTransaction(commandMapper.toCreateTransactionCommand(request));

    responseObserver.onNext(grpcMapper.toCreateTransactionGrpcResponse(transactionResult));
    responseObserver.onCompleted();
  }

  @Override
  public void watchTransactionStatus(
      WatchTransactionStatusRequest request,
      StreamObserver<TransactionStatusResponse> responseObserver) {
    UUID transactionId = UUID.fromString(request.getTransactionId());
    UUID authUserId = UUID.fromString(request.getAuthUserId());
    UUID subscriptionKey = UUID.fromString(request.getSubscriptionKey());

    ServerCallStreamObserver<TransactionStatusResponse> serverObserver =
        (ServerCallStreamObserver<TransactionStatusResponse>) responseObserver;

    serverObserver.setOnCancelHandler(
        () -> transactionStatusStreamRegistry.unsubscribe(transactionId, subscriptionKey));

    transactionStatusStreamRegistry.subscribe(
        transactionId, subscriptionKey, authUserId, serverObserver);
  }

  @Override
  public void getTransactionsByAccounts(
      GetTransactionsByAccountsGrpcRequest request,
      StreamObserver<GetTransactionsByAccountsGrpcResponse> responseObserver) {
    Map<UUID, AccountResponse> accountsById =
        request.getAccountsList().stream()
            .collect(
                Collectors.toMap(
                    account -> UUID.fromString(account.getAccountId()),
                    Function.identity(),
                    (first, ignored) -> first));

    Map<UUID, AccountResponse> fetchedAccountsById = new HashMap<>();
    var transactions =
        transactionService.getTransactionsByAccountIds(accountsById.keySet()).stream()
            .map(
                transaction -> {
                  AccountResponse sourceAccount =
                      accountsById.get(transaction.getSourceAccountId());
                  if (sourceAccount == null) {
                    sourceAccount =
                        fetchedAccountsById.computeIfAbsent(
                            transaction.getSourceAccountId(), accountGrpcClient::getAccountById);
                  }

                  AccountResponse targetAccount =
                      fetchedAccountsById.computeIfAbsent(
                          transaction.getTargetAccountId(), accountGrpcClient::getAccountById);
                  return grpcMapper.toTransactionResponse(
                      transaction, sourceAccount, targetAccount);
                })
            .toList();
    var response =
        GetTransactionsByAccountsGrpcResponse.newBuilder().addAllTransactions(transactions).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
