package notificationservice.grpc;

import io.grpc.stub.StreamObserver;
import notification.contract.v1.GetNotificationHealthGrpcRequest;
import notification.contract.v1.GetNotificationHealthGrpcResponse;
import notification.contract.v1.NotificationRpcServiceGrpc;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationGrpcService extends NotificationRpcServiceGrpc.NotificationRpcServiceImplBase {

    @Override
    public void getNotificationHealth(GetNotificationHealthGrpcRequest request, StreamObserver<GetNotificationHealthGrpcResponse> responseObserver) {
        GetNotificationHealthGrpcResponse response = GetNotificationHealthGrpcResponse.newBuilder().setMessage("Notification service GRPC health " + LocalDateTime.now()).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
