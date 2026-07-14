package authservice.dto;

import java.util.UUID;

public record LogoutCommand(
        String refreshToken
) {
}
