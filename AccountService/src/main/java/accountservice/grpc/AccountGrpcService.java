package accountservice.grpc;

import account.contract.v1.AccountRpcServiceGrpc;
import account.contract.v1.GetAccountRequest;
import account.contract.v1.GetAccountResponse;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class AccountGrpcService extends AccountRpcServiceGrpc.AccountRpcServiceImplBase {

    @Override
    public void getAccount(GetAccountRequest request, StreamObserver<GetAccountResponse> responseObserver) {
        GetAccountResponse response = GetAccountResponse.newBuilder().setMessage("Account service GRPC").build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
