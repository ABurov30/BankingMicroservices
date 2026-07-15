package userservice.grpc;


import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import user.contract.v1.*;

import java.time.LocalDateTime;

@Service
public class UserGrpcService extends UserRpcServiceGrpc.UserRpcServiceImplBase {

    @Override
    public void getUserHealth(GetUserHealthGrpcRequest request, StreamObserver<GetUserHealthGrpcResponse> responseObserver) {
        GetUserHealthGrpcResponse response = GetUserHealthGrpcResponse.newBuilder().setMessage("User service GRPC health " + LocalDateTime.now()).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
