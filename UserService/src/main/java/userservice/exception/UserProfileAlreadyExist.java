package userservice.exception;

public class UserProfileAlreadyExist extends RuntimeException{
    public UserProfileAlreadyExist () {
        super("User profile already exists");
    }
}
