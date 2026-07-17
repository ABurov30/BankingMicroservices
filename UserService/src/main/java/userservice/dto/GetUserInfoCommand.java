package userservice.dto;

import java.util.UUID;

public record GetUserInfoCommand(
        UUID authUserId
) {
}
