package notificationservice.service.push;

import notificationservice.enums.push.PushNotificationType;
import org.springframework.stereotype.Service;

@Service
public class PushNotificationResolver {
    public String resolveTitle (PushNotificationType type) {
        return switch (type) {
            case ACCOUNT_CREATED -> "Лицевой счет создан";
            case ACCOUNT_FROZEN -> "Лицевой счет заморожен";
            case ACCOUNT_UNFROZEN -> "Лицевой счет разморожен";
            case CARD_CREATED -> "Карта создана";
            case CARD_FROZEN -> "Карта заморожена";
            case CARD_UNFROZEN -> "Карта разморожена";
        };
    }

    public String resolveBody(PushNotificationType type, String accountNumber, String cardNumber) {
        return switch (type) {
            case ACCOUNT_CREATED -> "Ваш лицевой счет " + accountNumber + " создан";
            case ACCOUNT_FROZEN -> "Ваш лицевой счет " + accountNumber + " заморожен";
            case ACCOUNT_UNFROZEN -> "Ваш лицевой счет " + accountNumber + " разморожен";
            case CARD_CREATED -> "Ваша карта " + cardNumber + " по счету " + accountNumber + " создана";
            case CARD_FROZEN -> "Ваша карта " + cardNumber + " по счету " + accountNumber + " заморожена";
            case CARD_UNFROZEN -> "Ваша карта " + cardNumber + " по счету " + accountNumber + " разморожена";
        };
    }
}
