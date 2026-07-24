package notificationservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import notificationservice.document.NotificationDocument;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailSenderService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final NotificationTemplateResolver templateResolver;

    public EmailSenderService (
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            NotificationTemplateResolver templateResolver
    ) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.templateResolver = templateResolver;
    }

    public void send(NotificationDocument notification) {
        String template = templateResolver.resolveTemplate(notification.getType());
        String subject = templateResolver.resolveSubject(notification.getType());

        Context context = new Context();
        context.setVariable("email", notification.getEmail());
        context.setVariable("verificationCode", notification.getVerificationCode());

        String html = templateEngine.process(template, context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setTo(notification.getEmail());
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException exception) {
            throw new RuntimeException("Failed to send email", exception);
        }
    }
}
