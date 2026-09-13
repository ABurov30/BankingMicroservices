package notificationservice.service.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import notificationservice.enums.email.EmailNotificationType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class EmailNotificationTemplateResolverTest {
  private final EmailNotificationTemplateResolver resolver =
      new EmailNotificationTemplateResolver();
  private static final Map<EmailNotificationType, String> PATH_MAP =
      Map.of(
          EmailNotificationType.AUTH_USER_BLOCKED, "email/auth-user-blocked",
          EmailNotificationType.AUTH_USER_UNLOCKED, "email/auth-user-unlocked",
          EmailNotificationType.AUTH_USER_CREATED, "email/auth-user-created",
          EmailNotificationType.AUTH_USER_VERIFIED, "email/auth-user-verified",
          EmailNotificationType.AUTH_USER_FORGET_PASSWORD, "email/auth-user-forget-password");

  private static final Map<EmailNotificationType, String> SUBJECT_MAP =
      Map.of(
          EmailNotificationType.AUTH_USER_CREATED, "Welcome",
          EmailNotificationType.AUTH_USER_BLOCKED, "Account blocked",
          EmailNotificationType.AUTH_USER_UNLOCKED, "Account unlocked",
          EmailNotificationType.AUTH_USER_VERIFIED, "Email verified",
          EmailNotificationType.AUTH_USER_FORGET_PASSWORD, "Password reset");

  @ParameterizedTest(name = "Should resolve path to template {0}")
  @EnumSource(EmailNotificationType.class)
  void shouldReturnRightTemplatePath(EmailNotificationType type) {
    String expectedPath = PATH_MAP.get(type);
    assertNotNull(expectedPath, "Path not found for " + type);

    String path = resolver.resolveTemplate(type);
    assertEquals(expectedPath, path);
  }

  @ParameterizedTest(name = "Should resolve subject {0}")
  @EnumSource(EmailNotificationType.class)
  void shouldReturnRightSubject(EmailNotificationType type) {
    String expectedSubject = SUBJECT_MAP.get(type);
    assertNotNull(expectedSubject, "Subject not found for " + type);

    String subject = resolver.resolveSubject(type);
    assertEquals(expectedSubject, subject);
  }
}
