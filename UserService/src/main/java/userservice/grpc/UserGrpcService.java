package userservice.grpc;


import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import user.contract.v1.*;

@Service
public class UserGrpcService extends UserRpcServiceGrpc.UserRpcServiceImplBase {

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        GetUserResponse response = GetUserResponse.newBuilder().setMessage("User service GRPC").build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
