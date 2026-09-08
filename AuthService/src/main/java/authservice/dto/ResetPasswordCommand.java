package authservice.dto;

public record ResetPasswordCommand(String resetPasswordToken, String newPassword) {}
