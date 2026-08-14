package notificationservice.service.push;

import notificationservice.dto.AccountPushNotificationPayload;
import notificationservice.dto.CardPushNotificationPayload;
import notificationservice.dto.TransactionPushNotificationPayload;
import notificationservice.enums.push.PushNotificationType;
import notificationservice.exception.InvalidPushNotificationPayloadException;
import org.springframework.stereotype.Component;

@Component
public class PushNotificationResolver {

  private <T> T requirePayload(
      PushNotificationType notificationType, Object payload, Class<T> expectedClass) {
    if (!expectedClass.isInstance(payload)) {
      throw new InvalidPushNotificationPayloadException(notificationType, expectedClass, payload);
    }

    return expectedClass.cast(payload);
  }

  public String resolveTitle(PushNotificationType type) {
    return switch (type) {
      case ACCOUNT_CREATED -> "Account created";
      case ACCOUNT_FROZEN -> "Account frozen";
      case ACCOUNT_UNFROZEN -> "Account unfrozen";
      case CARD_CREATED -> "Card created";
      case CARD_FROZEN -> "Card frozen";
      case CARD_UNFROZEN -> "Card unfrozen";
      case TRANSACTION_FAILED -> "Transaction failed";
      case TRANSACTION_COMPLETED -> "Transaction completed";
      case TRANSACTION_RECEIVED -> "Funds received";
    };
  }

  public String resolveBody(PushNotificationType type, Object payload) {
    return switch (type) {
      case ACCOUNT_CREATED, ACCOUNT_FROZEN, ACCOUNT_UNFROZEN -> {
        AccountPushNotificationPayload accountPayload =
            requirePayload(type, payload, AccountPushNotificationPayload.class);

        yield switch (type) {
          case ACCOUNT_CREATED ->
              "Your account " + accountPayload.accountNumber() + " has been created";
          case ACCOUNT_FROZEN ->
              "Your account " + accountPayload.accountNumber() + " has been frozen";
          case ACCOUNT_UNFROZEN ->
              "Your account " + accountPayload.accountNumber() + " has been unfrozen";
          default -> throw new IllegalStateException();
        };
      }

      case CARD_CREATED, CARD_FROZEN, CARD_UNFROZEN -> {
        CardPushNotificationPayload cardPayload =
            requirePayload(type, payload, CardPushNotificationPayload.class);

        yield switch (type) {
          case CARD_CREATED ->
              "Your card "
                  + cardPayload.cardNumber()
                  + " for account "
                  + cardPayload.accountNumber()
                  + " has been created";
          case CARD_FROZEN -> "Your card " + cardPayload.cardNumber() + " has been frozen";
          case CARD_UNFROZEN -> "Your card " + cardPayload.cardNumber() + " has been unfrozen";
          default -> throw new IllegalStateException();
        };
      }

      case TRANSACTION_FAILED, TRANSACTION_COMPLETED, TRANSACTION_RECEIVED -> {
        TransactionPushNotificationPayload transactionPayload =
            requirePayload(type, payload, TransactionPushNotificationPayload.class);

        yield switch (type) {
          case TRANSACTION_RECEIVED ->
              "Your account "
                  + transactionPayload.accountNumber()
                  + " has been credited with "
                  + transactionPayload.amount();
          case TRANSACTION_COMPLETED ->
              "The transaction from account "
                  + transactionPayload.accountNumber()
                  + " in the amount of "
                  + transactionPayload.amount()
                  + " has been completed";
          case TRANSACTION_FAILED ->
              "Transaction in the amount of " + transactionPayload.amount() + " has failed";
          default -> throw new IllegalStateException();
        };
      }
    };
  }
}
