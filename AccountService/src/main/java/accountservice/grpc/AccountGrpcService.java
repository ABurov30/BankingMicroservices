package accountservice.grpc;

import account.contract.v1.AccountRpcServiceGrpc;
import account.contract.v1.CreateAccountGrpcRequest;
import account.contract.v1.CreateAccountGrpcResponse;
import account.contract.v1.GetAccountHealthGrpcResponse;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AccountGrpcService extends AccountRpcServiceGrpc.AccountRpcServiceImplBase {

    @Override
    public void getAccountHealth(Empty request, StreamObserver<GetAccountHealthGrpcResponse> responseObserver) {
        GetAccountHealthGrpcResponse response = GetAccountHealthGrpcResponse.newBuilder().setMessage("Account service GRPC health " + LocalDateTime.now()).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void createAccount(CreateAccountGrpcRequest request, StreamObserver<CreateAccountGrpcResponse> responseObserver) {

    }
}
