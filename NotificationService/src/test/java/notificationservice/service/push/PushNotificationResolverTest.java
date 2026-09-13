package notificationservice.service.push;

import static org.junit.jupiter.api.Assertions.*;

import enums.common.Currency;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import notificationservice.dto.AccountPushNotificationPayload;
import notificationservice.dto.CardPushNotificationPayload;
import notificationservice.dto.TransactionPushNotificationPayload;
import notificationservice.enums.push.PushNotificationType;
import notificationservice.exception.InvalidPushNotificationPayloadException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

public class PushNotificationResolverTest {
  private final PushNotificationResolver resolver = new PushNotificationResolver();

  private static final String ACCOUNT_NUMBER = "account12345678910";
  private static final String CARD_NUMBER = "card12345678910";
  private static final UUID ACCOUNT_ID = UUID.randomUUID();
  private static final BigDecimal AMOUNT = new BigDecimal("100");
  private static final String CURRENCY = "¥";
  private static final AccountPushNotificationPayload ACCOUNT_PUSH_PAYLOAD =
      new AccountPushNotificationPayload(ACCOUNT_ID, ACCOUNT_NUMBER);
  private static final CardPushNotificationPayload CARD_PUSH_PAYLOAD =
      new CardPushNotificationPayload(ACCOUNT_ID, ACCOUNT_NUMBER, CARD_NUMBER);
  private static final TransactionPushNotificationPayload TRANSACTION_PAYLOAD =
      new TransactionPushNotificationPayload(ACCOUNT_NUMBER, AMOUNT, Currency.CNY);

  private static final Map<PushNotificationType, String> TITLE_MAP =
      Map.of(
          PushNotificationType.ACCOUNT_CREATED, "Account created",
          PushNotificationType.ACCOUNT_FROZEN, "Account frozen",
          PushNotificationType.ACCOUNT_UNFROZEN, "Account unfrozen",
          PushNotificationType.CARD_CREATED, "Card created",
          PushNotificationType.CARD_FROZEN, "Card frozen",
          PushNotificationType.CARD_UNFROZEN, "Card unfrozen",
          PushNotificationType.TRANSACTION_FAILED, "Transaction failed",
          PushNotificationType.TRANSACTION_COMPLETED, "Transaction completed",
          PushNotificationType.TRANSACTION_RECEIVED, "Funds received");

  private static final Map<PushNotificationType, String> EXPECTED_BODY_MAP =
      Map.of(
          PushNotificationType.ACCOUNT_CREATED,
          "Your account " + ACCOUNT_NUMBER + " has been created",
          PushNotificationType.ACCOUNT_FROZEN,
          "Your account " + ACCOUNT_NUMBER + " has been frozen",
          PushNotificationType.ACCOUNT_UNFROZEN,
          "Your account " + ACCOUNT_NUMBER + " has been unfrozen",
          PushNotificationType.CARD_CREATED,
          "Your card " + CARD_NUMBER + " for account " + ACCOUNT_NUMBER + " has been created",
          PushNotificationType.CARD_FROZEN,
          "Your card " + CARD_NUMBER + " has been frozen",
          PushNotificationType.CARD_UNFROZEN,
          "Your card " + CARD_NUMBER + " has been unfrozen",
          PushNotificationType.TRANSACTION_RECEIVED,
          "Your account " + ACCOUNT_NUMBER + " has been credited with " + AMOUNT + CURRENCY,
          PushNotificationType.TRANSACTION_COMPLETED,
          "The transaction from account "
              + ACCOUNT_NUMBER
              + " in the amount of "
              + AMOUNT
              + CURRENCY
              + " has been completed",
          PushNotificationType.TRANSACTION_FAILED,
          "Transaction in the amount of " + AMOUNT + CURRENCY + " has failed");

  @ParameterizedTest(name = "Should resolve title for {0}")
  @EnumSource(PushNotificationType.class)
  void shouldReturnRightTitle(PushNotificationType type) {
    String expectedTitle = TITLE_MAP.get(type);
    assertNotNull(expectedTitle, "Не задан ожидаемый заголовок для " + type);

    String title = resolver.resolveTitle(type);
    assertEquals(expectedTitle, title);
  }

  @ParameterizedTest(name = "Should resolve body for {0}")
  @MethodSource("payloadProvider")
  void shouldResolveBodyForNotificationType(
      PushNotificationType type, Object payload, String expectedBody) {
    String actualBody = resolver.resolveBody(type, payload);
    assertEquals(expectedBody, actualBody);
  }

  @ParameterizedTest(name = "Should use the correct currency symbol in the body for {0}")
  @CsvSource({"USD, $", "EUR, €", "GBP, £", "CNY, ¥"})
  void shouldResolveRightCurrencySymbol(Currency currency, String expectedSymbol) {
    TransactionPushNotificationPayload payload =
        new TransactionPushNotificationPayload(ACCOUNT_NUMBER, AMOUNT, currency);
    String body = resolver.resolveBody(PushNotificationType.TRANSACTION_RECEIVED, payload);
    String expectedBody =
        "Your account " + ACCOUNT_NUMBER + " has been credited with " + AMOUNT + expectedSymbol;
    assertEquals(expectedBody, body);
  }

  static Stream<Arguments> payloadProvider() {
    return Stream.of(
        Arguments.of(
            PushNotificationType.ACCOUNT_CREATED,
            ACCOUNT_PUSH_PAYLOAD,
            EXPECTED_BODY_MAP.get(PushNotificationType.ACCOUNT_CREATED)),
        Arguments.of(
            PushNotificationType.ACCOUNT_FROZEN,
            ACCOUNT_PUSH_PAYLOAD,
            EXPECTED_BODY_MAP.get(PushNotificationType.ACCOUNT_FROZEN)),
        Arguments.of(
            PushNotificationType.ACCOUNT_UNFROZEN,
            ACCOUNT_PUSH_PAYLOAD,
            EXPECTED_BODY_MAP.get(PushNotificationType.ACCOUNT_UNFROZEN)),
        Arguments.of(
            PushNotificationType.CARD_CREATED,
            CARD_PUSH_PAYLOAD,
            EXPECTED_BODY_MAP.get(PushNotificationType.CARD_CREATED)),
        Arguments.of(
            PushNotificationType.CARD_FROZEN,
            CARD_PUSH_PAYLOAD,
            EXPECTED_BODY_MAP.get(PushNotificationType.CARD_FROZEN)),
        Arguments.of(
            PushNotificationType.CARD_UNFROZEN,
            CARD_PUSH_PAYLOAD,
            EXPECTED_BODY_MAP.get(PushNotificationType.CARD_UNFROZEN)),
        Arguments.of(
            PushNotificationType.TRANSACTION_FAILED,
            TRANSACTION_PAYLOAD,
            EXPECTED_BODY_MAP.get(PushNotificationType.TRANSACTION_FAILED)),
        Arguments.of(
            PushNotificationType.TRANSACTION_COMPLETED,
            TRANSACTION_PAYLOAD,
            EXPECTED_BODY_MAP.get(PushNotificationType.TRANSACTION_COMPLETED)),
        Arguments.of(
            PushNotificationType.TRANSACTION_RECEIVED,
            TRANSACTION_PAYLOAD,
            EXPECTED_BODY_MAP.get(PushNotificationType.TRANSACTION_RECEIVED)));
  }

  @ParameterizedTest(name = "Should throw an exception when payload is null for {0}")
  @EnumSource(PushNotificationType.class)
  void shouldThrowExceptionWhenPayloadEqualsNull(PushNotificationType type) {
    assertThrows(
        InvalidPushNotificationPayloadException.class, () -> resolver.resolveBody(type, null));
  }

  @ParameterizedTest(name = "Should throw an exception when payload type does not match {0}")
  @MethodSource("wrongPayloadProvider")
  void shouldThrowExceptionWhenPayloadTypeDoesNotMatchNotificationType(
      PushNotificationType type, Object payload) {
    assertThrows(
        InvalidPushNotificationPayloadException.class, () -> resolver.resolveBody(type, payload));
  }

  static Stream<Arguments> wrongPayloadProvider() {
    return Stream.of(
        Arguments.of(PushNotificationType.CARD_CREATED, TRANSACTION_PAYLOAD),
        Arguments.of(PushNotificationType.ACCOUNT_CREATED, CARD_PUSH_PAYLOAD),
        Arguments.of(PushNotificationType.TRANSACTION_FAILED, ACCOUNT_PUSH_PAYLOAD));
  }
}
