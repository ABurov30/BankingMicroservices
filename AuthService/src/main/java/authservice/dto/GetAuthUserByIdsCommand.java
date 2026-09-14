package authservice.dto;

import java.util.List;
import java.util.UUID;

public record GetAuthUserByIdsCommand(List<UUID> authUserIds) {}
