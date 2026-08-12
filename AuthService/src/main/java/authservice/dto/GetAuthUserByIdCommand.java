package authservice.dto;

import java.util.UUID;

public record GetAuthUserByIdCommand(UUID authUserId) {}
