package notificationservice.service.email;

import notificationservice.enums.email.EmailNotificationType;
import org.springframework.stereotype.Service;



@Service
public class EmailNotificationTemplateResolver {

    public String resolveTemplate(EmailNotificationType type) {
        return switch (type) {
            case AUTH_USER_BLOCKED -> "email/auth-user-blocked";
            case AUTH_USER_UNLOCKED -> "email/auth-user-unlocked";
            case AUTH_USER_CREATED -> "email/auth-user-created";
            case AUTH_USER_VERIFIED -> "email/auth-user-verified";
            case AUTH_USER_FORGET_PASSWORD -> "email/auth-user-forget-password";
        };
    }

    public String resolveSubject(EmailNotificationType type) {
        return switch (type) {
            case AUTH_USER_CREATED -> "Welcome";
            case AUTH_USER_BLOCKED -> "Account blocked";
            case AUTH_USER_UNLOCKED -> "Account unlocked";
            case AUTH_USER_VERIFIED -> "Email verified";
            case AUTH_USER_FORGET_PASSWORD -> "Password reset";
        };
    }
}
