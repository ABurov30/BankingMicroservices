package notificationservice.service.push;

import notificationservice.enums.email.EmailNotificationType;
import notificationservice.enums.push.PushNotificationType;
import org.springframework.stereotype.Service;

@Service
public class PushNotificationResolver {
    public String resolveTitle (PushNotificationType type) {
        return switch (type) {
            case ACCOUNT_CREATED -> "Лицевой счет создан";
            case ACCOUNT_FROZEN -> "Лицевой счет заморожен";
            case ACCOUNT_UNFROZEN -> "Лицевой счет разморожен";
        };
    }

    public String resolveBody (PushNotificationType type) {
        return switch (type) {
            case ACCOUNT_CREATED -> "Ваш лицевой счет создан";
            case ACCOUNT_FROZEN -> "Ваш лицевой счет заморожен";
            case ACCOUNT_UNFROZEN -> "Ваш лицевой счет разморожен";
        };
    }
}
