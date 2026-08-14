package notificationservice.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import notificationservice.document.EmailNotificationDocument;
import notificationservice.exception.EmailSendFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailSenderService {
  private static final Logger log = LoggerFactory.getLogger(EmailSenderService.class);
  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final EmailNotificationTemplateResolver templateResolver;
  private final String siteUrl;

  public EmailSenderService(
      JavaMailSender mailSender,
      TemplateEngine templateEngine,
      EmailNotificationTemplateResolver templateResolver,
      @Value("${site.url}") String siteUrl) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
    this.templateResolver = templateResolver;
    this.siteUrl = siteUrl;
  }

  public void send(EmailNotificationDocument notification) {
    String template = templateResolver.resolveTemplate(notification.getType());
    String subject = templateResolver.resolveSubject(notification.getType());

    Context context = new Context();
    context.setVariable("email", notification.getEmail());
    context.setVariable("authUserId", notification.getAuthUserId());
    context.setVariable("verificationCode", notification.getVerificationCode());
    context.setVariable("siteUrl", siteUrl);
    context.setVariable("verifyUrl", buildVerifyUrl(notification));
    context.setVariable("resetPasswordUrl", buildResetPasswordUrl(notification));

    String html = templateEngine.process(template, context);

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

      helper.setTo(notification.getEmail().trim());
      helper.setSubject(subject);
      helper.setText(html, true);
      helper.setFrom("no-reply@buro-bank.ru", "Buro Bank");
      mailSender.send(message);
    } catch (MessagingException | UnsupportedEncodingException exception) {
      log.error("Failed to send email notification: type={}", notification.getType(), exception);
      throw new EmailSendFailedException(exception);
    }
  }

  private String buildVerifyUrl(EmailNotificationDocument notification) {
    return siteUrl.replaceAll("/+$", "")
        + "/user-verify/"
        + notification.getAuthUserId()
        + "/"
        + notification.getVerificationCode();
  }

  private String buildResetPasswordUrl(EmailNotificationDocument notification) {
    return siteUrl.replaceAll("/+$", "") + "/reset-password/" + notification.getAuthUserId();
  }
}
