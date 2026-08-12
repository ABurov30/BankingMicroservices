package cardservice.dto;

import java.util.UUID;
import org.springframework.lang.Nullable;

public record CreatedCardCommand(
    UUID accountId,
    @Nullable UUID authUserId,
    @Nullable String accountNumber,
    @Nullable String role) {}
