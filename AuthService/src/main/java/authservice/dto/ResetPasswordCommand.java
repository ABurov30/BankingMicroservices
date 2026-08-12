package authservice.dto;

import java.util.UUID;

public record ResetPasswordCommand(UUID authUserId, String newPassword) {}
