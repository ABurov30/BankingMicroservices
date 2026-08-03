package cardservice.dto;
import org.springframework.lang.Nullable;

import java.util.UUID;

public record CreatedCardCommand(
        UUID accountId,
        @Nullable
        UUID authUserId,
        @Nullable
        String accountNumber,
        @Nullable
        String role
) {
}
