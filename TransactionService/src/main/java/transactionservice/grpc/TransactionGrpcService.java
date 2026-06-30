package transactionservice.grpc;

import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import transaction.contract.v1.GetTransactionRequest;
import transaction.contract.v1.GetTransactionResponse;
import transaction.contract.v1.TransactionRpcServiceGrpc;

@Service
public class TransactionGrpcService
    extends TransactionRpcServiceGrpc.TransactionRpcServiceImplBase {

  @Override
  public void getTransaction(
      GetTransactionRequest request, StreamObserver<GetTransactionResponse> responseObserver) {
    GetTransactionResponse response =
        GetTransactionResponse.newBuilder().setMessage("Transaction service GRPC").build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
