package notificationservice.service.email;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import notificationservice.document.EmailNotificationDocument;
import notificationservice.enums.email.EmailNotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class EmailSenderServiceTest {
  private final JavaMailSender mailSender = mock(JavaMailSender.class);
  private final TemplateEngine templateEngine = createTemplateEngine();
  private final EmailNotificationTemplateResolver templateResolver =
      new EmailNotificationTemplateResolver();
  private final EmailSenderService senderService =
      new EmailSenderService(mailSender, templateEngine, templateResolver, "https://bank.example");

  private static final String EMAIL = "test@gmail.com";
  private static final UUID AUTH_USER_ID = UUID.randomUUID();
  private static final String VERIFICATION_CODE = "12345";
  private static final String VERIFICATION_WRONG_CODE = "00000";

  private static final Map<EmailNotificationType, String> SUBJECT_MAP =
      Map.of(
          EmailNotificationType.AUTH_USER_CREATED, "Welcome",
          EmailNotificationType.AUTH_USER_BLOCKED, "Account blocked",
          EmailNotificationType.AUTH_USER_UNLOCKED, "Account unlocked",
          EmailNotificationType.AUTH_USER_VERIFIED, "Email verified",
          EmailNotificationType.AUTH_USER_FORGET_PASSWORD, "Password reset");

  private static TemplateEngine createTemplateEngine() {
    ClassLoaderTemplateResolver resourceResolver = new ClassLoaderTemplateResolver();
    resourceResolver.setPrefix("templates/");
    resourceResolver.setSuffix(".html");
    resourceResolver.setTemplateMode(TemplateMode.HTML);
    resourceResolver.setCharacterEncoding("UTF-8");

    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resourceResolver);
    return engine;
  }

  private EmailNotificationDocument prepareNotification(EmailNotificationType type, String email) {
    EmailNotificationDocument notification = new EmailNotificationDocument();
    notification.setEmail(email);
    notification.setAuthUserId(AUTH_USER_ID);
    notification.setType(type);
    notification.setVerificationCode(VERIFICATION_CODE);
    return notification;
  }

  private boolean isShouldContainsVerificationCode(EmailNotificationType type) {
    return type.equals(EmailNotificationType.AUTH_USER_FORGET_PASSWORD)
        || type.equals(EmailNotificationType.AUTH_USER_CREATED);
  }

  @ParameterizedTest(name = "Should send right message for {0}")
  @EnumSource(EmailNotificationType.class)
  public void shouldSendRightMessage(EmailNotificationType type)
      throws MessagingException, IOException {
    var notification = prepareNotification(type, EMAIL);
    MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));

    when(mailSender.createMimeMessage()).thenReturn(message);
    senderService.send(notification);

    verify(mailSender).send(message);
    assertEquals(SUBJECT_MAP.get(type), message.getSubject());

    Address[] recipients = message.getRecipients(Message.RecipientType.TO);
    assertNotNull(recipients);
    assertEquals(1, recipients.length);
    assertEquals(EMAIL, ((InternetAddress) recipients[0]).getAddress());

    String html = (String) message.getContent();
    assertTrue(html.contains(EMAIL));

    if (!isShouldContainsVerificationCode(type)) {
      return;
    }

    assertTrue(html.contains(VERIFICATION_CODE));

    String expectedVerifyUrl = "";
    if (type.equals(EmailNotificationType.AUTH_USER_CREATED)) {
      assertTrue(
          html.contains(">" + VERIFICATION_CODE + "</div>"),
          "Verification code should be displayed in the email");

      expectedVerifyUrl =
          "https://bank.example/user-verify/" + AUTH_USER_ID + "/" + VERIFICATION_CODE;

      assertTrue(html.contains("href=\"" + expectedVerifyUrl + "\""));
    } else if (type.equals(EmailNotificationType.AUTH_USER_FORGET_PASSWORD)) {
      expectedVerifyUrl = "https://bank.example/reset-password?token=" + VERIFICATION_CODE;
    }

    assertTrue(html.contains("href=\"" + expectedVerifyUrl + "\""));
  }

  @Test
  void shouldPropagateSendException() throws MessagingException {
    var notification = prepareNotification(EmailNotificationType.AUTH_USER_CREATED, EMAIL);
    MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));

    when(mailSender.createMimeMessage()).thenReturn(message);

    MailSendException failure = new MailSendException("SMTP unavailable");

    doThrow(failure).when(mailSender).send(message);

    MailSendException actual =
        assertThrows(MailSendException.class, () -> senderService.send(notification));
    assertSame(failure, actual);
  }

  @Test
  void shouldTrimRecipientEmail() throws MessagingException {
    var emailWithWhitespaces = " " + EMAIL + " ";
    var notification =
        prepareNotification(EmailNotificationType.AUTH_USER_UNLOCKED, emailWithWhitespaces);
    MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));

    when(mailSender.createMimeMessage()).thenReturn(message);
    senderService.send(notification);

    verify(mailSender).send(message);

    Address[] recipients = message.getRecipients(Message.RecipientType.TO);
    assertNotNull(recipients);
    assertEquals(1, recipients.length);
    assertEquals(EMAIL, ((InternetAddress) recipients[0]).getAddress());
  }
}
