package authservice.grpc;

import auth.contract.v1.AuthRpcServiceGrpc;
import auth.contract.v1.GetAuthRequest;
import auth.contract.v1.GetAuthResponse;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class AuthGrpcService extends AuthRpcServiceGrpc.AuthRpcServiceImplBase {

    @Override
    public void getAuth(GetAuthRequest request, StreamObserver<GetAuthResponse> responseObserver) {
        GetAuthResponse response = GetAuthResponse.newBuilder().setMessage("Auth service GRPC").build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
