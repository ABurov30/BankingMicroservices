package accountservice.dto;

import java.util.UUID;

public record GetAccountByIdCommand(UUID accountId) {}
