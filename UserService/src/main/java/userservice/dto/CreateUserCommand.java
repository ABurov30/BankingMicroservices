package userservice.dto;

import enums.user.UserProfileStatus;
import java.util.UUID;

public record CreateUserCommand(
    UUID authUserId,
    String email,
    String firstName,
    String lastName,
    UserProfileStatus status,
    String role) {}
