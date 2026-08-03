package apigateway.client;

import com.google.protobuf.Empty;
import apigateway.dto.notification.GetNotificationsRequestDto;
import apigateway.dto.notification.MarkNotificationsAsReadedCommand;
import apigateway.dto.notification.NotificationResponseDto;
import notification.contract.v1.GetNotificationHealthGrpcResponse;
import notification.contract.v1.NotificationRpcServiceGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class NotificationGrpcClient {
    private final NotificationRpcServiceGrpc.NotificationRpcServiceBlockingStub stub;
    private final RestClient notificationRestClient;

    public NotificationGrpcClient(
            NotificationRpcServiceGrpc.NotificationRpcServiceBlockingStub stub,
            RestClient.Builder restClientBuilder,
            @Value("${NOTIFICATION_GRPC_HOST}") String notificationHost,
            @Value("${NOTIFICATION_PORT}") int notificationPort
    ) {
        this.stub = stub;
        this.notificationRestClient = restClientBuilder
                .baseUrl("http://" + notificationHost + ":" + notificationPort)
                .build();
    }

    public String getNotificationHealth() {
        GetNotificationHealthGrpcResponse response =
                stub.withDeadlineAfter(2, TimeUnit.SECONDS)
                        .getNotificationHealth(Empty.getDefaultInstance());
        return response.getMessage();
    }

    public List<NotificationResponseDto> getNotifications(GetNotificationsRequestDto request) {
        NotificationResponseDto[] response = notificationRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/notification/notifications")
                        .queryParam("authUserId", request.authUserId())
                        .build())
                .retrieve()
                .body(NotificationResponseDto[].class);

        return response == null ? List.of() : Arrays.asList(response);
    }

    public void markAsReaded(MarkNotificationsAsReadedCommand command) {
        notificationRestClient.patch()
                .uri("/notification/notifications/mark-as-readed")
                .body(command)
                .retrieve()
                .toBodilessEntity();
    }
}
