package apigateway.dto.result.user;

import java.util.UUID;

public record GetRecipientResultDto(
    UUID userProfileId, String email, String firstName, String lastName) {}
