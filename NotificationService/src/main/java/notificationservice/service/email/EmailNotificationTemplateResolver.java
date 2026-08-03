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
            case AUTH_USER_CREATED -> "Добро пожаловать";
            case AUTH_USER_BLOCKED -> "Аккаунт заблокирован";
            case AUTH_USER_UNLOCKED -> "Аккаунт разблокирован";
            case AUTH_USER_VERIFIED -> "Email подтвержден";
            case AUTH_USER_FORGET_PASSWORD -> "Пароль сброшен";
        };
    }
}
