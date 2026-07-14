package authservice.dto;

public record SignupCommand(
    String email,
    String password,
    String firstName,
    String lastName
) {
}
