package apigateway.controller;

import apigateway.client.NotificationGrpcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notification")
public class NotificationGatewayController {

    private final NotificationGrpcClient notificationClient;

    public NotificationGatewayController(NotificationGrpcClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    @GetMapping("/health")
    public String getNotificationHealth() {
        return notificationClient.getNotificationHealth();
    }
}
