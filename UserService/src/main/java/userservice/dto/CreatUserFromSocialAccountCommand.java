package userservice.dto;

import java.util.UUID;

public record CreatUserFromSocialAccountCommand(
    UUID authUserId, String email, String firstName, String lastName) {}
