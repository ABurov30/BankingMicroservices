package userservice.dto;

import java.util.UUID;

public record BlockedUserCommand(UUID authUserId) {}
