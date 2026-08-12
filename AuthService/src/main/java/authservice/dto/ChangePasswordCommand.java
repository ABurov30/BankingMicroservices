package authservice.dto;

import java.util.UUID;

public record ChangePasswordCommand(UUID authUserId, String oldPassword, String newPassword) {}
