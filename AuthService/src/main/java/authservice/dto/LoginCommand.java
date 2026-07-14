package authservice.dto;

public record LoginCommand(
        String email,
        String password
) {
}
