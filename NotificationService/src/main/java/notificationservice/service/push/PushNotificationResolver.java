package notificationservice.service.push;

import notificationservice.enums.push.PushNotificationType;
import org.springframework.stereotype.Service;

@Service
public class PushNotificationResolver {
    public String resolveTitle (PushNotificationType type) {
        return switch (type) {
            case ACCOUNT_CREATED -> "Account created";
            case ACCOUNT_FROZEN -> "Account frozen";
            case ACCOUNT_UNFROZEN -> "Account unfrozen";
            case CARD_CREATED -> "Card created";
            case CARD_FROZEN -> "Card frozen";
            case CARD_UNFROZEN -> "Card unfrozen";
        };
    }

    public String resolveBody(PushNotificationType type, String accountNumber, String cardNumber) {
        return switch (type) {
            case ACCOUNT_CREATED -> "Your account " + accountNumber + " has been created";
            case ACCOUNT_FROZEN -> "Your account " + accountNumber + " has been frozen";
            case ACCOUNT_UNFROZEN -> "Your account " + accountNumber + " has been unfrozen";
            case CARD_CREATED -> "Your card " + cardNumber + " for account " + accountNumber + " has been created";
            case CARD_FROZEN -> "Your card " + cardNumber + " for account " + accountNumber + " has been frozen";
            case CARD_UNFROZEN -> "Your card " + cardNumber + " for account " + accountNumber + " has been unfrozen";
        };
    }
}
