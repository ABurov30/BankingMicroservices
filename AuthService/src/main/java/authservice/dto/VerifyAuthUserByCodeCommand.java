package authservice.dto;

import java.util.UUID;

public record VerifyAuthUserByCodeCommand(UUID authUserId, String verificationCode) {}
