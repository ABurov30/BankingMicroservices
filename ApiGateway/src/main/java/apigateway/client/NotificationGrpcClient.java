package apigateway.client;

import notification.contract.v1.GetNotificationHealthGrpcRequest;
import notification.contract.v1.GetNotificationHealthGrpcResponse;
import notification.contract.v1.NotificationRpcServiceGrpc;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class NotificationGrpcClient {
    private final NotificationRpcServiceGrpc.NotificationRpcServiceBlockingStub stub;

    public NotificationGrpcClient(NotificationRpcServiceGrpc.NotificationRpcServiceBlockingStub stub) {
        this.stub = stub;
    }

    public String getNotificationHealth() {
        GetNotificationHealthGrpcResponse response =
                stub.withDeadlineAfter(2, TimeUnit.SECONDS)
                        .getNotificationHealth(GetNotificationHealthGrpcRequest.newBuilder().build());
        return response.getMessage();
    }
}
