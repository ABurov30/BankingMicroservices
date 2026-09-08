package transactionservice.grpc;

import com.google.protobuf.Empty;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import transaction.contract.v1.*;
import transactionservice.dto.CreateTransactionResult;
import transactionservice.mapper.command.TransactionCommandMapper;
import transactionservice.mapper.grpc.TransactionGrpcMapper;
import transactionservice.service.TransactionService;

@Service
public class TransactionGrpcService
    extends TransactionRpcServiceGrpc.TransactionRpcServiceImplBase {

  private final TransactionService transactionService;
  private final TransactionCommandMapper commandMapper;
  private final TransactionGrpcMapper grpcMapper;
  private final TransactionStatusStreamRegistry transactionStatusStreamRegistry;

  public TransactionGrpcService(
      TransactionService transactionService,
      TransactionCommandMapper transactionCommandMapper,
      TransactionGrpcMapper transactionGrpcMapper,
      TransactionStatusStreamRegistry transactionStatusStreamRegistry) {
    this.transactionService = transactionService;
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
    var transactions = transactionService.getTransactionsByAccountIds(request.getAccountsList());

    responseObserver.onNext(grpcMapper.toGetTransactionsByAccountsGrpcResponse(transactions));
    responseObserver.onCompleted();
  }
}
