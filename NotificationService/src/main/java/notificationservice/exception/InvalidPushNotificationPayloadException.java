package notificationservice.exception;

import notificationservice.enums.push.PushNotificationType;

public class InvalidPushNotificationPayloadException extends RuntimeException {
  public InvalidPushNotificationPayloadException(
      PushNotificationType notificationType, Class<?> expectedClass, Object payload) {
    super(
        "Payload for "
            + notificationType
            + " must be "
            + expectedClass.getSimpleName()
            + ", but received "
            + (payload == null ? "null" : payload.getClass().getSimpleName()));
  }
}
