package transactionservice.grpc;

import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import transaction.contract.v1.*;

import java.time.LocalDateTime;

@Service
public class TransactionGrpcService
    extends TransactionRpcServiceGrpc.TransactionRpcServiceImplBase {

  @Override
  public void getTransactionHealth(
          GetTransactionHealthGrpcRequest request, StreamObserver<GetTransactionHealthGrpcResponse> responseObserver) {
    GetTransactionHealthGrpcResponse response =
        GetTransactionHealthGrpcResponse.newBuilder().setMessage("Transaction service GRPC health " + LocalDateTime.now()).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
