package notificationservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import notificationservice.document.NotificationDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;

@Service
public class EmailSenderService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final NotificationTemplateResolver templateResolver;
    private final String siteUrl;

    public EmailSenderService (
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            NotificationTemplateResolver templateResolver,
            @Value("${site.url}") String siteUrl
    ) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.templateResolver = templateResolver;
        this.siteUrl = siteUrl;
    }

    public void send(NotificationDocument notification) {
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
            throw new RuntimeException("Failed to send email", exception);
        }
    }

    private String buildVerifyUrl(NotificationDocument notification) {
        return siteUrl.replaceAll("/+$", "")
                + "/user-verify/"
                + notification.getAuthUserId()
                + "/"
                + notification.getVerificationCode();
    }

    private String buildResetPasswordUrl(NotificationDocument notification) {
        return siteUrl.replaceAll("/+$", "")
                + "/reset-password/"
                + notification.getAuthUserId();
    }
}
